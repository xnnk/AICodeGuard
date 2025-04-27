package com.ai.aicodeguard.infrastructure.ai.streaming;

import com.ai.aicodeguard.infrastructure.ai.StreamingResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * @ClassName: DefaultStreamingResponseHandler
 * @Description: 默认流式响应处理器
 * @Author: LZX
 * @Date: 2025/4/26 12:24
 */
@Slf4j
public class DefaultStreamingResponseHandler implements StreamingResponseHandler {

    private final StringBuilder completeMessage = new StringBuilder();
    private static final String EVENT_MESSAGE = "message";
    private static final String EVENT_ERROR = "error";
    private static final String EVENT_DONE = "done";

    @Override
    public boolean handleResponse(String content, SseEmitter emitter) {
        try {
            // 判断是否是结束标记
            boolean isLast = content.endsWith("[DONE]");
            if (isLast) {
                content = content.replace("[DONE]", "");
            }

            if (!content.isEmpty()) {
                // 累积完整消息
                completeMessage.append(content);

                // 发送事件消息
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name(EVENT_MESSAGE)
                    .data(content, MediaType.TEXT_PLAIN);
                emitter.send(event);
            }

            // 如果是最后一条消息，发送完成事件
            if (isLast) {
                SseEmitter.SseEventBuilder doneEvent = SseEmitter.event()
                    .name(EVENT_DONE)
                    .data(completeMessage.toString(), MediaType.TEXT_PLAIN);
                emitter.send(doneEvent);
                return true;
            }

            return false;
        } catch (IOException e) {
            log.error("发送SSE消息失败", e);
            handleError(e, emitter);
            return true;
        }
    }

    @Override
    public void handleError(Throwable error, SseEmitter emitter) {
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name(EVENT_ERROR)
                .data(error.getMessage(), MediaType.TEXT_PLAIN);
            emitter.send(event);
            emitter.complete();
        } catch (IOException e) {
            log.error("发送SSE错误消息失败", e);
            emitter.completeWithError(e);
        }
    }

    public String getCompleteMessage() {
        return completeMessage.toString();
    }
}
