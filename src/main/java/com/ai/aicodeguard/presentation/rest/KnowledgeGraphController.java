package com.ai.aicodeguard.presentation.rest;

import com.ai.aicodeguard.infrastructure.graph.KnowledgeGraphService;
import com.ai.aicodeguard.presentation.response.WebResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName: KnowledgeGraphController
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/28 16:04
 */
@RestController
@RequestMapping("/knowledge-graph")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    /**
     * 根据编程语言和漏洞类型查询相关代码模式
     * @param language
     * @param vulnerabilityType
     * @return
     */
    @GetMapping("/code-patterns")
    public WebResponse getCodePatterns(
            @RequestParam String language,
            @RequestParam String vulnerabilityType) {
        try {
            String patterns = knowledgeGraphService.findCodePatternsForVulnerability(language, vulnerabilityType);
            return WebResponse.success(patterns);
        } catch (Exception e) {
            log.error("查询代码模式失败", e);
            return WebResponse.fail("查询失败: " + e.getMessage());
        }
    }
}
