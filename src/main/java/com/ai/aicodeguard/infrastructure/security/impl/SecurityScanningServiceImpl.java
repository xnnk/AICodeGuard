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
import com.ai.aicodeguard.infrastructure.security.SecurityScanningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

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

    private final SecurityScanFactory securityScanFactory;
    private final GeneratedCodeRepository generatedCodeRepository;
    private final GeneratedCodeDocumentRepository mongoGeneratedCodeRepository;
    private final DetectionTaskRepository detectionTaskRepository;
    private final VulnerabilityReportRepository vulnerabilityReportRepository;
    private final KnowledgeGraphService knowledgeGraphService;

    @Override
    public void scanGeneratedCode(String codeId) {
        try {
            log.info("开始扫描代码: {}", codeId);

            // 获取代码内容
            Optional<GeneratedCode> codeOptional = generatedCodeRepository.findById(codeId);
            Optional<GeneratedCodeDocument> documentOptional = mongoGeneratedCodeRepository.findById(codeId);

            if (codeOptional.isEmpty() || documentOptional.isEmpty()) {
                log.error("找不到需要扫描的代码: {}", codeId);
                return;
            }

            GeneratedCode code = codeOptional.get();
            GeneratedCodeDocument document = documentOptional.get();

            // 更新为扫描中状态
            code.setScanStatus(GeneratedCode.ScanStatus.PENDING);
            generatedCodeRepository.save(code);
            document.setScanStatus("PENDING");
            mongoGeneratedCodeRepository.save(document);

            // 获取默认扫描服务
            SecurityScanService scanService = securityScanFactory.getDefaultService();

            // 执行扫描
            String taskId = scanService.scanCode(codeId, document.getContent(), document.getLanguage());

            log.info("代码扫描任务已创建: {} -> {}", codeId, taskId);

        } catch (Exception e) {
            log.error("启动代码安全扫描失败", e);
            // 更新为扫描失败状态
            updateScanStatus(codeId, GeneratedCode.ScanStatus.FAILED);
        }
    }

    @Override
    public VulnerabilityReport getScanResult(String codeId) {
        log.info("获取代码扫描结果: {}", codeId);

        // 查找最新的扫描任务
        Optional<DetectionTask> taskOptional = detectionTaskRepository.findFirstByCodeIdOrderByStartTimeDesc(codeId);
        if (taskOptional.isEmpty()) {
            log.warn("找不到代码{}的扫描任务", codeId);
            return null;
        }

        DetectionTask task = taskOptional.get();

        // 如果任务已完成，返回结果
        if (task.getStatus() == DetectionTask.TaskStatus.SUCCESS) {
            Optional<VulnerabilityReport> reportOptional = vulnerabilityReportRepository.findByCodeId(codeId);
            if (reportOptional.isPresent()) {
                // 更新扫描状态
                updateScanStatus(codeId, GeneratedCode.ScanStatus.SUCCESS);

                // 更新知识图谱 - 新增逻辑
                VulnerabilityReport report = reportOptional.get();
                try {
                    knowledgeGraphService.updateGraphWithVulnerabilities(codeId, report);
                } catch (Exception e) {
                    // 仅记录错误，不影响正常流程
                    log.error("更新知识图谱失败", e);
                }

            }
        } else if (task.getStatus() == DetectionTask.TaskStatus.FAILED) {
            // 更新为扫描失败状态
            updateScanStatus(codeId, GeneratedCode.ScanStatus.FAILED);
        }

        return null;
    }

    /**
     * 更新代码的扫描状态
     */
    private void updateScanStatus(String codeId, GeneratedCode.ScanStatus status) {
        try {
            Optional<GeneratedCode> codeOptional = generatedCodeRepository.findById(codeId);
            Optional<GeneratedCodeDocument> documentOptional = mongoGeneratedCodeRepository.findById(codeId);

            if (codeOptional.isPresent()) {
                GeneratedCode code = codeOptional.get();
                code.setScanStatus(status);
                code.setScanTime(LocalDateTime.now());
                generatedCodeRepository.save(code);
            }

            if (documentOptional.isPresent()) {
                GeneratedCodeDocument document = documentOptional.get();
                document.setScanStatus(status.name());
                mongoGeneratedCodeRepository.save(document);
            }
        } catch (Exception e) {
            log.error("更新代码扫描状态失败: {}", codeId, e);
        }
    }
}
