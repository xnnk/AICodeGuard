package com.ai.aicodeguard.infrastructure.security;

import com.ai.aicodeguard.domain.codegen.document.VulnerabilityReport;

/**
 * @ClassName: SecurityScanService
 * @Description: 安全扫描服务
 * @Author: LZX
 * @Date: 2025/4/27 17:42
 */
public interface SecurityScanService {
    /**
     * 执行代码安全扫描
     * @param codeId 要扫描的代码ID
     * @param content 代码内容
     * @param language 代码语言
     * @return 扫描任务ID
     */
    String scanCode(String codeId, String content, String language);

    /**
     * 获取扫描结果
     * @param taskId 扫描任务ID
     * @return 漏洞报告
     */
    VulnerabilityReport getScanResult(String taskId);

    /**
     * 获取扫描服务类型
     */
    SecurityScanType getType();
}
