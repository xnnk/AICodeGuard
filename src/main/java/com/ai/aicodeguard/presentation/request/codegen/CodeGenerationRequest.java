package com.ai.aicodeguard.presentation.request.codegen;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @ClassName: CodeGenerationRequest
 * @Description: 代码生成请求DTO
 * @Author: LZX
 * @Date: 2025/4/20 00:44
 */
@Data
public class CodeGenerationRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    /**
     * 自然语言需求
     */
    @NotBlank(message = "需求描述不能为空")
    private String prompt;

    /**
     * 目标编程语言
     */
    @NotBlank(message = "编程语言不能为空")
    private String language;

    /**
     * AI模型类型
     */
    private String modelType;
}
