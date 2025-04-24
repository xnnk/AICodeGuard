package com.ai.aicodeguard.presentation.response.codegen;

import lombok.Builder;
import lombok.Data;

/**
 * @ClassName: CodeGenerationResponse
 * @Description: 代码生成响应DTO
 * @Author: LZX
 * @Date: 2025/4/20 00:46
 */
@Data
@Builder
public class CodeGenerationResponse {

    /**
     * 代码ID
     */
    private String codeId;

    /**
     * 生成的代码内容
     */
    private String content;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 使用的AI模型
     */
    private String modelUsed;
}
