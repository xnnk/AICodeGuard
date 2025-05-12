package com.ai.aicodeguard.infrastructure.security.impl;

import com.ai.aicodeguard.domain.codegen.DetectionTask;
import com.ai.aicodeguard.domain.codegen.GeneratedCode;
import com.ai.aicodeguard.domain.codegen.document.GeneratedCodeDocument;
import com.ai.aicodeguard.domain.codegen.document.VulnerabilityReport;
import com.ai.aicodeguard.infrastructure.graph.KnowledgeGraphService;
import com.ai.aicodeguard.infrastructure.mongo.GeneratedCodeDocumentRepository;
import com.ai.aicodeguard.infrastructure.mongo.VulnerabilityReportRepository;
import com.ai.aicodeguard.infrastructure.persistence.DetectionTaskRepository;
import com.ai.aicodeguard.infrastructure.persistence.GeneratedCodeRepository;
import com.ai.aicodeguard.infrastructure.security.SecurityScanFactory;
import com.ai.aicodeguard.infrastructure.security.SecurityScanService;
import com.ai.aicodeguard.infrastructure.security.SecurityScanType;
import com.ai.aicodeguard.infrastructure.security.SecurityScanningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * @ClassName: SecurityScanningServiceImpl
 * @Description: 代码安全扫描服务实现类
 * @Author: LZX
 * @Date: 2025/4/27 17:49
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityScanningServiceImpl implements SecurityScanningService {

    private final GeneratedCodeRepository codeRepository;
    private final GeneratedCodeDocumentRepository codeDocumentRepository;
    private final DetectionTaskRepository taskRepository;
    private final VulnerabilityReportRepository reportRepository;
    private final AIModelScanServiceImpl aiModelScanService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Value("${security.scan.default-type}")
    private SecurityScanType defaultScanType;

    @Override
    public void scanGeneratedCode(String codeId) {
        log.info("开始扫描代码: {}", codeId);

        // 1. 查找代码文档
        Optional<GeneratedCodeDocument> codeDocOpt = codeDocumentRepository.findById(codeId);
        if (codeDocOpt.isEmpty()) {
            throw new RuntimeException("未找到指定的代码: " + codeId);
        }

        // 2. 验证MySQL中是否存在对应记录
        Optional<GeneratedCode> codeEntityOpt = codeRepository.findById(codeId);
        if (codeEntityOpt.isEmpty()) {
            throw new RuntimeException("未找到对应的代码元数据记录: " + codeId);
        }

        GeneratedCodeDocument document = codeDocOpt.get();
        String language = document.getLanguage();
        String content = document.getContent();

        // 3. 创建扫描任务
        String taskId = UUID.randomUUID().toString();
        DetectionTask task = new DetectionTask();
        task.setId(taskId);
        task.setCodeId(codeId);
        task.setStartTime(LocalDateTime.now());
        task.setStatus(DetectionTask.TaskStatus.PENDING);
        taskRepository.save(task);

        log.info("代码扫描任务已创建: {} -> {}", codeId, taskId);

        // 4. 使用单一线程处理整个流程，确保顺序执行
        taskExecutor.execute(() -> {
            try {
                // 执行AI模型扫描代码
                log.info("开始执行AI模型扫描，代码ID: {}", codeId);
                String aiScanTaskId = aiModelScanService.scanCode(codeId, content, language);

                // 轮询等待AI扫描完成
                VulnerabilityReport report = null;
                int maxRetries = 30; // 最多等待30次，每次2秒
                int retryCount = 0;

                while (report == null && retryCount < maxRetries) {
                    try {
                        Thread.sleep(2000);
                        report = reportRepository.findAllByCodeId(codeId)
                                .stream()
                                // 列表中只留下最后一个扫描结果
                                .reduce((first, second) -> second)
                                .orElse(null);;
                        if (report != null) {
                            break;
                        }
                        retryCount++;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("等待AI扫描结果时被中断", e);
                    }
                }

                if (report == null) {
                    log.warn("无法获取AI扫描结果，可能扫描未完成或失败: {}", codeId);
                    updateTaskStatus(taskId, DetectionTask.TaskStatus.FAILED, "无法获取AI扫描结果");
                    updateCodeScanStatus(codeId, GeneratedCode.ScanStatus.FAILED);
                    return;
                }

                // AI扫描完成后，更新知识图谱
                log.info("AI扫描完成，发现{}个漏洞，开始更新知识图谱...",
                        report.getVulnerabilities() != null ? report.getVulnerabilities().size() : 0);

                if (report.getVulnerabilities() != null && !report.getVulnerabilities().isEmpty()) {
                    knowledgeGraphService.updateGraphWithVulnerabilities(codeId, report);
                    log.info("知识图谱更新完成，代码ID: {}", codeId);
                } else {
                    log.info("没有发现漏洞，无需更新知识图谱");
                }

                // 更新任务状态和代码扫描状态
                updateTaskStatus(taskId, DetectionTask.TaskStatus.SUCCESS, null);
                updateCodeScanStatus(codeId, GeneratedCode.ScanStatus.SUCCESS);

            } catch (Exception e) {
                log.error("代码扫描或知识图谱更新失败: {}", codeId, e);
                updateTaskStatus(taskId, DetectionTask.TaskStatus.FAILED, e.getMessage());
                updateCodeScanStatus(codeId, GeneratedCode.ScanStatus.FAILED);
            }
        });

        log.info("代码扫描和知识图谱更新任务已提交，代码ID: {}", codeId);
    }

    @Override
    public VulnerabilityReport getScanResult(String codeId) {
        return reportRepository.findByCodeId(codeId).orElse(null);
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, DetectionTask.TaskStatus status, String errorMessage) {
        Optional<DetectionTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
            DetectionTask task = taskOpt.get();
            task.setStatus(status);
            task.setEndTime(LocalDateTime.now());
            if (errorMessage != null) {
                task.setErrorMessage(errorMessage);
            }
            taskRepository.save(task);
        }
    }

    /**
     * 更新代码扫描状态
     */
    private void updateCodeScanStatus(String codeId, GeneratedCode.ScanStatus status) {
        Optional<GeneratedCode> codeOpt = codeRepository.findById(codeId);
        if (codeOpt.isPresent()) {
            GeneratedCode code = codeOpt.get();
            code.setScanStatus(status);
            code.setScanTime(LocalDateTime.now());
            codeRepository.save(code);
        }
    }
}
