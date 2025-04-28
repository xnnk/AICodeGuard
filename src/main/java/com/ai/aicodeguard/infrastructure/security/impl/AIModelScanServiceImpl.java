package com.ai.aicodeguard.infrastructure.security.impl;

import com.ai.aicodeguard.domain.codegen.DetectionTask;
import com.ai.aicodeguard.domain.codegen.document.VulnerabilityReport;
import com.ai.aicodeguard.infrastructure.ai.AIClientFactory;
import com.ai.aicodeguard.infrastructure.ai.AIClientService;
import com.ai.aicodeguard.infrastructure.ai.conversation.ChatMessage;
import com.ai.aicodeguard.infrastructure.mongo.VulnerabilityReportRepository;
import com.ai.aicodeguard.infrastructure.persistence.DetectionTaskRepository;
import com.ai.aicodeguard.infrastructure.security.SecurityScanService;
import com.ai.aicodeguard.infrastructure.security.SecurityScanType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @ClassName: AIModelScanServiceImpl
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/27 17:46
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIModelScanServiceImpl implements SecurityScanService {

    private final AIClientFactory aiClientFactory;
    private final DetectionTaskRepository detectionTaskRepository;
    private final VulnerabilityReportRepository vulnerabilityReportRepository;

    @Override
    public String scanCode(String codeId, String content, String language) {
        // 创建检测任务
        String taskId = UUID.randomUUID().toString();
        DetectionTask task = new DetectionTask();
        task.setId(taskId);
        task.setCodeId(codeId);
        task.setStatus(DetectionTask.TaskStatus.PENDING);
        task.setStartTime(LocalDateTime.now());
        detectionTaskRepository.save(task);

        // 使用新线程执行扫描，避免阻塞
        Thread scanThread = new Thread(() -> {
            try {
                log.info("开始AI代码安全扫描，代码ID: {}, 任务ID: {}", codeId, task.getId());

                // 从配置中获取默认AI模型客户端
                // 这里直接使用claude模型而不是通过getDefaultClient来避免空指针问题
                AIClientService aiClient = aiClientFactory.getClient("claude");

                // 构建安全扫描提示词
                String prompt = buildSecurityPrompt(content, language);

                // 调用AI模型进行安全分析
                String analysisResult = aiClient.generateCode(prompt, "text");

                // 解析AI响应，创建漏洞报告
                VulnerabilityReport report = parseAnalysisResult(codeId, analysisResult);

                // 保存报告到数据库
                vulnerabilityReportRepository.save(report);

                // 更新任务状态为成功
                task.setStatus(DetectionTask.TaskStatus.SUCCESS);
                task.setEndTime(LocalDateTime.now());
                detectionTaskRepository.save(task);

                log.info("AI代码安全扫描完成，代码ID: {}, 任务ID: {}", codeId, task.getId());
            } catch (Exception e) {
                log.error("代码安全扫描失败", e);
                // 更新任务状态为失败
                task.setStatus(DetectionTask.TaskStatus.FAILED);
                task.setEndTime(LocalDateTime.now());
                task.setErrorMessage(e.getMessage());
                detectionTaskRepository.save(task);
            }
        });

        // 启动扫描线程
        scanThread.start();

        // 返回任务ID
        return task.getId();
    }

    @Override
    public VulnerabilityReport getScanResult(String taskId) {
        DetectionTask task = detectionTaskRepository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != DetectionTask.TaskStatus.SUCCESS) {
            return null;
        }

        return vulnerabilityReportRepository.findByCodeId(task.getCodeId()).get();
    }

    @Override
    public SecurityScanType getType() {
        return SecurityScanType.AI_MODEL;
    }

    /**
     * 构建安全扫描提示词
     */
    private String buildSecurityPrompt(String codeContent, String language) {
        return String.format(
                """
                请对以下%s代码进行安全分析，识别可能的安全漏洞、代码缺陷或最佳实践问题：
                
                ```%s
                %s
                ```
                
                请以JSON格式返回分析结果，格式如下：
                ```json
                {
                  "vulnerabilities": [
                    {
                      "type": "漏洞类型名称",
                      "severity": "CRITICAL/HIGH/MEDIUM/LOW",
                      "line": 行号,
                      "description": "问题详细描述",
                      "suggestion": "修复建议"
                    }
                  ],
                  "summary": "总体安全评估"
                }
                ```
                
                仅返回JSON响应，不要包含任何其他文本。如果代码没有安全问题，请返回空的vulnerabilities数组。
                """,
                language, language, codeContent
        );
    }

    /**
     * 解析AI模型返回的分析结果
     */
    private VulnerabilityReport parseAnalysisResult(String codeId, String analysisResult) {
        try {
            VulnerabilityReport report = new VulnerabilityReport();
            report.setId(UUID.randomUUID().toString());
            report.setCodeId(codeId);
            report.setScanTime(LocalDateTime.now());

            // 提取JSON部分
            String jsonContent = extractJsonContent(analysisResult);

            // 解析JSON成漏洞列表
            List<VulnerabilityReport.Vulnerability> vulnerabilities = parseVulnerabilities(jsonContent);
            report.setVulnerabilities(vulnerabilities);

            return report;
        } catch (Exception e) {
            log.error("解析安全分析结果失败", e);
            throw new RuntimeException("解析安全分析结果失败: " + e.getMessage());
        }
    }

    /**
     * 从AI输出中提取JSON内容
     */
    private String extractJsonContent(String content) {
        // 处理可能的JSON代码块
        if (content.contains("```json")) {
            int start = content.indexOf("```json") + 7;
            int end = content.indexOf("```", start);
            if (end > start) {
                return content.substring(start, end).trim();
            }
        }

        // 尝试提取{}包围的内容
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}') + 1;
        if (start >= 0 && end > start) {
            return content.substring(start, end);
        }

        return content;
    }

    /**
     * 解析漏洞列表
     */
    private List<VulnerabilityReport.Vulnerability> parseVulnerabilities(String jsonContent) {
        // 使用Jackson解析JSON
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonContent);

            List<VulnerabilityReport.Vulnerability> vulnerabilities = new ArrayList<>();

            if (root.has("vulnerabilities")) {
                JsonNode vulnArray = root.get("vulnerabilities");
                for (JsonNode vulnNode : vulnArray) {
                    VulnerabilityReport.Vulnerability vulnerability = new VulnerabilityReport.Vulnerability();
                    vulnerability.setType(vulnNode.has("type") ? vulnNode.get("type").asText() : "未知");
                    vulnerability.setLine(vulnNode.has("line") ? vulnNode.get("line").asInt() : 0);
                    vulnerability.setSeverity(vulnNode.has("severity") ?
                            VulnerabilityReport.Severity.valueOf(vulnNode.get("severity").asText()) :
                            VulnerabilityReport.Severity.MEDIUM);
                    vulnerability.setSuggestion(vulnNode.has("suggestion") ? vulnNode.get("suggestion").asText() : "");

                    vulnerabilities.add(vulnerability);
                }
            }

            return vulnerabilities;
        } catch (Exception e) {
            log.error("解析漏洞JSON失败", e);
            throw new RuntimeException("解析漏洞JSON失败: " + e.getMessage());
        }
    }
}
