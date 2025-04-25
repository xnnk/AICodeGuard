package com.ai.aicodeguard.infrastructure.mongo;

import com.ai.aicodeguard.domain.conversation.ConversationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @ClassName: ConversationRepository
 * @Description: MongoDB存储库接口，用于操作对话记录
 * @Author: LZX
 * @Date: 2025/4/24 17:38
 */
@Repository
public interface ConversationRepository extends MongoRepository<ConversationDocument, String> {

    Optional<ConversationDocument> findByUserIdAndConversationId(String userId, String conversationId);

    List<ConversationDocument> findByUserIdAndActiveIsTrue(String userId);

    List<ConversationDocument> findByLastUpdatedAtBeforeAndActiveIsTrue(long timestamp);
}
