package com.ai.aicodeguard.infrastructure.ai.conversation;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @ClassName: Conversation
 * @Description: 对话记录类
 * @Author: LZX
 * @Date: 2025/4/20 01:56
 */
@Data
public class Conversation {
    private String id;
    private String userId;
    private String modelType;
    private List<ChatMessage> messages = new ArrayList<>();
    private long createdAt;
    private long lastUpdatedAt;

    public Conversation(String userId, String modelType) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.modelType = modelType;
        this.createdAt = System.currentTimeMillis();
        this.lastUpdatedAt = this.createdAt;

        // 添加系统消息作为对话的开始
        this.messages.add(new ChatMessage("system", "你是一个有用的AI助手，专注于帮助用户解决编程问题。"));
    }

    /**
     * 添加用户消息
     */
    public void addUserMessage(String content) {
        this.messages.add(new ChatMessage("user", content));
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    /**
     * 添加AI回复
     */
    public void addAssistantMessage(String content) {
        this.messages.add(new ChatMessage("assistant", content));
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    /**
     * 获取最后一条消息
     */
    public ChatMessage getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
}
