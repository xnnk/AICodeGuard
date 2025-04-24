package com.ai.aicodeguard.presentation.rest;

import com.ai.aicodeguard.domain.codegen.document.GeneratedCodeDocument;
import com.ai.aicodeguard.domain.codegen.service.CodeGenerationService;
import com.ai.aicodeguard.infrastructure.common.util.ShiroUtils;
import com.ai.aicodeguard.presentation.request.codegen.CodeGenerationRequest;
import com.ai.aicodeguard.presentation.response.WebResponse;
import com.ai.aicodeguard.presentation.response.codegen.CodeGenerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * @ClassName: CodeGenerationController
 * @Description: 代码生成控制器
 * @Author: LZX
 * @Date: 2025/4/20 00:43
 */
@RestController
@RequestMapping("/code-gen")
@RequiredArgsConstructor
@Slf4j
public class CodeGenerationController {

    private final CodeGenerationService codeGenerationService;

    /**
     * 生成代码接口
     */
    @PostMapping("/generate")
    public WebResponse generateCode(@Valid @RequestBody CodeGenerationRequest request) {
        try {
            // 从安全上下文获取用户ID
            Integer userId = ShiroUtils.getUserId();
            if (userId == null) {
                return WebResponse.fail("未登录或会话已过期，请重新登录");
            }

            // 调用代码生成服务
            GeneratedCodeDocument generatedCode = codeGenerationService.generateCode(
                request.getPrompt(),
                request.getLanguage(),
                userId,
                request.getModelType()  // 传入模型类型
            );

            // 构建响应
            CodeGenerationResponse response = CodeGenerationResponse.builder()
                .codeId(generatedCode.getId())
                .content(generatedCode.getContent())
                .language(generatedCode.getLanguage())
                .modelUsed(generatedCode.getAiModel())  // 添加使用的模型信息
                .build();

            return WebResponse.success(response);
        } catch (Exception e) {
            log.error("代码生成失败", e);
            return WebResponse.fail("代码生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取支持的AI模型列表
     */
    @GetMapping("/models")
    public WebResponse getSupportedModels() {
        return WebResponse.success(java.util.Arrays.stream(com.ai.aicodeguard.infrastructure.ai.AIModelType.values())
            .map(Enum::name)
            .toList());
    }
}
