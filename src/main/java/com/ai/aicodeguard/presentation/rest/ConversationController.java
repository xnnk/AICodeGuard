package com.ai.aicodeguard.presentation.rest;

import com.ai.aicodeguard.infrastructure.ai.AIClientFactory;
import com.ai.aicodeguard.infrastructure.ai.AIClientService;
import com.ai.aicodeguard.infrastructure.ai.StreamingResponseHandler;
import com.ai.aicodeguard.infrastructure.ai.conversation.Conversation;
import com.ai.aicodeguard.infrastructure.ai.conversation.ConversationManager;
import com.ai.aicodeguard.infrastructure.ai.streaming.DefaultStreamingResponseHandler;
import com.ai.aicodeguard.infrastructure.common.util.ShiroUtils;
import com.ai.aicodeguard.presentation.request.codegen.CreateConversationRequest;
import com.ai.aicodeguard.presentation.request.codegen.SendMessageRequest;
import com.ai.aicodeguard.presentation.request.codegen.StreamMessageRequest;
import com.ai.aicodeguard.presentation.response.WebResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: ConversationController
 * @Description: 对话控制器
 * @Author: LZX
 * @Date: 2025/4/20 02:09
 */
@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final AIClientFactory aiClientFactory;
    private final ConversationManager conversationManager;

    /**
     * 创建对话
     */
    @PostMapping("/create")
    public WebResponse createConversation(@Valid @RequestBody CreateConversationRequest request) {
        try {
            AIClientService aiClient = aiClientFactory.getClient(request.getModelType());
            Conversation conversation = aiClient.createConversation(Objects.requireNonNull(ShiroUtils.getUserId()).toString());

            return WebResponse.success(Map.of(
                "conversationId", conversation.getId(),
                "modelType", conversation.getModelType()
            ));
        } catch (Exception e) {
            log.error("创建对话失败", e);
            return WebResponse.fail("创建对话失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public WebResponse sendMessage(@Valid @RequestBody SendMessageRequest request) {
        try {
            Conversation conversation = conversationManager.getConversation(
                String.valueOf(ShiroUtils.getUserId()),
                request.getConversationId()
            );

            if (conversation == null) {
                return WebResponse.fail("对话不存在或已过期");
            }

            AIClientService aiClient = aiClientFactory.getClient(conversation.getModelType());
            String reply = aiClient.sendMessage(conversation, request.getMessage());

            return WebResponse.success(Map.of(
                "reply", reply,
                "conversationId", conversation.getId()
            ));
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return WebResponse.fail("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 流式发送消息
     * @param request
     * @return
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(@Valid @RequestBody StreamMessageRequest request) {
        // 创建SSE发射器，设置超时时间为3分钟
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(3));

        try {
            Conversation conversation = conversationManager.getConversation(
                String.valueOf(ShiroUtils.getUserId()),
                request.getConversationId()
            );

            if (conversation == null) {
                emitter.completeWithError(new RuntimeException("对话不存在或已过期"));
                return emitter;
            }

            // 创建流处理器
            StreamingResponseHandler handler = new DefaultStreamingResponseHandler();

            // 异步处理流式响应
            AIClientService aiClient = aiClientFactory.getClient(conversation.getModelType());

            // 设置完成回调
            emitter.onCompletion(() -> log.info("流式对话完成: {}", request.getConversationId()));
            emitter.onTimeout(() -> log.warn("流式对话超时: {}", request.getConversationId()));
            emitter.onError(e -> log.error("流式对话出错: {}", request.getConversationId(), e));

            // 异步发送消息
            CompletableFuture.runAsync(() -> {
                try {
                    aiClient.sendMessageStreaming(conversation, request.getMessage(), emitter, handler);
                } catch (Exception e) {
                    log.error("发送流式消息失败", e);
                    handler.handleError(e, emitter);
                }
            });

        } catch (Exception e) {
            log.error("初始化流式对话失败", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/{userId}/{conversationId}")
    public WebResponse getConversationHistory(
            @PathVariable Integer userId,
            @PathVariable String conversationId) {
        try {
            Conversation conversation = conversationManager.getConversation(
                userId.toString(),
                conversationId
            );

            if (conversation == null) {
                return WebResponse.fail("对话不存在或已过期");
            }

            return WebResponse.success(conversation);
        } catch (Exception e) {
            log.error("获取对话历史失败", e);
            return WebResponse.fail("获取对话历史失败: " + e.getMessage());
        }
    }

    /**
     * 结束对话
     */
    @DeleteMapping("/{userId}/{conversationId}")
    public WebResponse endConversation(@PathVariable Integer userId, @PathVariable String conversationId) {
        try {
            conversationManager.removeConversation(userId.toString(), conversationId);
            return WebResponse.successWithMessage("对话已结束");
        } catch (Exception e) {
            log.error("结束对话失败", e);
            return WebResponse.fail("结束对话失败: " + e.getMessage());
        }
    }
}
