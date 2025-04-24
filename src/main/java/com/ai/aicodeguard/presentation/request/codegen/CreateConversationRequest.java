package com.ai.aicodeguard.presentation.request.codegen;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @ClassName: CreateConversationRequest
 * @Description: 创建对话请求DTO
 * @Author: LZX
 * @Date: 2025/4/20 02:11
 */
@Data
public class CreateConversationRequest {
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    private String modelType;
}
