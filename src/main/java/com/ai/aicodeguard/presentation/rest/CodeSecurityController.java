package com.ai.aicodeguard.presentation.rest;

import com.ai.aicodeguard.domain.codegen.document.VulnerabilityReport;
import com.ai.aicodeguard.infrastructure.security.SecurityScanningService;
import com.ai.aicodeguard.presentation.response.WebResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName: CodeSecurityController
 * @Description: 代码安全扫描控制器
 * @Author: LZX
 * @Date: 2025/4/27 17:53
 */
@RestController
@RequestMapping("/code-security")
@RequiredArgsConstructor
@Slf4j
public class CodeSecurityController {

    private final SecurityScanningService securityScanningService;

    /**
     * 触发代码安全扫描
     */
    @PostMapping("/scan/{codeId}")
    public WebResponse scanCode(@PathVariable String codeId) {
        try {
            securityScanningService.scanGeneratedCode(codeId);
            return WebResponse.successWithMessage("代码安全扫描已启动");
        } catch (Exception e) {
            log.error("触发代码安全扫描失败", e);
            return WebResponse.fail("触发代码安全扫描失败: " + e.getMessage());
        }
    }

    /**
     * 获取扫描结果
     */
    @GetMapping("/result/{codeId}")
    public WebResponse getScanResult(@PathVariable String codeId) {
        try {
            VulnerabilityReport report = securityScanningService.getScanResult(codeId);

            if (report == null) {
                return WebResponse.fail("扫描未完成或未找到结果");
            }

            return WebResponse.success(report);
        } catch (Exception e) {
            log.error("获取扫描结果失败", e);
            return WebResponse.fail("获取扫描结果失败: " + e.getMessage());
        }
    }
}
