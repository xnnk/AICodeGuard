package com.ai.aicodeguard.infrastructure.security;

import com.ai.aicodeguard.domain.codegen.document.VulnerabilityReport;

/**
 * @InterfaceName: SecurityScanningService
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/27 17:56
 */
public interface SecurityScanningService {
    /**
     * 扫描生成的代码
     * @param codeId 代码ID
     */
    void scanGeneratedCode(String codeId);

    /**
     * 获取扫描结果
     * @param codeId 代码ID
     * @return 漏洞报告
     */
    VulnerabilityReport getScanResult(String codeId);
}
