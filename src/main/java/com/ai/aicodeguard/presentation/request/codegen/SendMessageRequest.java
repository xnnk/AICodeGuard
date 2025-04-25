package com.ai.aicodeguard.presentation.request.codegen;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @ClassName: SendMessageRequest
 * @Description: 发送消息请求DTO
 * @Author: LZX
 * @Date: 2025/4/20 02:11
 */
@Data
public class SendMessageRequest {

    @NotBlank(message = "对话ID不能为空")
    private String conversationId;

    @NotBlank(message = "消息内容不能为空")
    private String message;
}
