package com.ai.aicodeguard.infrastructure.ai.conversation;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName: ConversationManager
 * @Description: 会话管理器
 * @Author: LZX
 * @Date: 2025/4/20 01:57
 */
@Component
public class ConversationManager {
    // 使用用户ID和对话ID的组合作为键
    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();

    /**
     * 创建新对话
     */
    public Conversation createConversation(String userId, String modelType) {
        Conversation conversation = new Conversation(userId, modelType);
        String key = buildKey(userId, conversation.getId());
        conversations.put(key, conversation);
        return conversation;
    }

    /**
     * 获取对话
     */
    public Conversation getConversation(String userId, String conversationId) {
        String key = buildKey(userId, conversationId);
        return conversations.get(key);
    }

    /**
     * 更新对话
     */
    public void updateConversation(String userId, Conversation conversation) {
        String key = buildKey(userId, conversation.getId());
        conversations.put(key, conversation);
    }

    /**
     * 删除对话
     */
    public void removeConversation(String userId, String conversationId) {
        String key = buildKey(userId, conversationId);
        conversations.remove(key);
    }

    /**
     * 构建键
     */
    private String buildKey(String userId, String conversationId) {
        return userId + ":" + conversationId;
    }
}
