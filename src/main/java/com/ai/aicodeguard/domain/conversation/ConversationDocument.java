package com.ai.aicodeguard.domain.conversation;

import com.ai.aicodeguard.infrastructure.ai.conversation.ChatMessage;
import com.ai.aicodeguard.infrastructure.ai.conversation.Conversation;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: ConversationDocument
 * @Description: MongoDB文档类，用于存储对话信息
 * @Author: LZX
 * @Date: 2025/4/24 17:31
 */
@Data
@Document(collection = "conversations")
@CompoundIndexes({
        @CompoundIndex(name = "userId_conversationId", def = "{'userId': 1, 'conversationId': 1}", unique = true)
})
public class ConversationDocument {
    @Id
    private String id;
    private String conversationId;
    private String userId;
    private String modelType;
    private List<ChatMessage> messages = new ArrayList<>();
    private long createdAt;
    private long lastUpdatedAt;
    private boolean active = true;

    // 从现有的 Conversation 对象创建文档
    public static ConversationDocument fromConversation(Conversation conversation) {
        ConversationDocument document = new ConversationDocument();
        document.setConversationId(conversation.getId());
        document.setUserId(conversation.getUserId());
        document.setModelType(conversation.getModelType());
        document.setMessages(conversation.getMessages());
        document.setCreatedAt(conversation.getCreatedAt());
        document.setLastUpdatedAt(conversation.getLastUpdatedAt());
        return document;
    }

    // 将文档转换回 Conversation 对象
    public Conversation toConversation() {
        Conversation conversation = new Conversation(this.userId, this.modelType);
        conversation.setId(this.conversationId);
        conversation.setMessages(this.messages);
        conversation.setCreatedAt(this.createdAt);
        conversation.setLastUpdatedAt(this.lastUpdatedAt);
        return conversation;
    }
}
