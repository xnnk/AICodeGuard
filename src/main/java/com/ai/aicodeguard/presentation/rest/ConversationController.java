package com.ai.aicodeguard.presentation.rest;

import com.ai.aicodeguard.infrastructure.ai.AIClientFactory;
import com.ai.aicodeguard.infrastructure.ai.AIClientService;
import com.ai.aicodeguard.infrastructure.ai.conversation.Conversation;
import com.ai.aicodeguard.infrastructure.ai.conversation.ConversationManager;
import com.ai.aicodeguard.presentation.request.codegen.CreateConversationRequest;
import com.ai.aicodeguard.presentation.request.codegen.SendMessageRequest;
import com.ai.aicodeguard.presentation.response.WebResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Map;

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
            Conversation conversation = aiClient.createConversation(request.getUserId().toString());

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
                request.getUserId().toString(),
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
