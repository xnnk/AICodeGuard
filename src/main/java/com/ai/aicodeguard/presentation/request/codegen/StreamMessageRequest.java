package com.ai.aicodeguard.presentation.request.codegen;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName: StreamMessageRequest
 * @Description: 流式消息请求体
 * @Author: LZX
 * @Date: 2025/4/26 12:42
 */
@Data
public class StreamMessageRequest {
    @NotBlank(message = "对话ID不能为空")
    private String conversationId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    // 可选参数，如果需要控制流式输出的行为
    private Double temperature;
    private Integer maxTokens;
}
