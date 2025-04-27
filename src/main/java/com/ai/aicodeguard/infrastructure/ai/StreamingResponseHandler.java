package com.ai.aicodeguard.infrastructure.ai;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @InterfaceName: StreamingResponseHandler
 * @Description: 流式响应处理器接口
 * @Author: LZX
 * @Date: 2025/4/26 12:21
 */
public interface StreamingResponseHandler {
    /**
     * 处理流式响应并发送到SSE通道
     * @param content 消息内容
     * @param emitter SSE发射器
     * @return 是否是最后一条消息
     */
    boolean handleResponse(String content, SseEmitter emitter);

    /**
     * 处理错误
     * @param error 错误信息
     * @param emitter SSE发射器
     */
    void handleError(Throwable error, SseEmitter emitter);
}
