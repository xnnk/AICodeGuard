package com.ai.aicodeguard.infrastructure.ai.conversation;

import com.ai.aicodeguard.domain.conversation.ConversationDocument;
import com.ai.aicodeguard.infrastructure.cache.ConversationCacheService;
import com.ai.aicodeguard.infrastructure.mongo.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * @ClassName: ConversationManager
 * @Description: 会话管理器
 * @Author: LZX
 * @Date: 2025/4/20 01:57
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationManager {

    private final ConversationRepository conversationRepository;
    private final ConversationCacheService cacheService;


    /**
     * 创建新的对话
     * @param userId
     * @param modelType
     * @return
     */
    public Conversation createConversation(String userId, String modelType) {
        Conversation conversation = new Conversation(userId, modelType);

        // 保存到MongoDB
        ConversationDocument document = ConversationDocument.fromConversation(conversation);
        conversationRepository.save(document);

        // 添加到缓存
        cacheService.cacheConversation(userId, conversation);

        return conversation;
    }

    /**
     * 获取对话
     * @param userId
     * @param conversationId
     * @return
     */
    public Conversation getConversation(String userId, String conversationId) {
        // 首先尝试从缓存获取
        Conversation conversation = cacheService.getConversation(userId, conversationId);

        if (conversation == null) {
            // 缓存未命中，从MongoDB获取
            Optional<ConversationDocument> optionalDocument =
                    conversationRepository.findByUserIdAndConversationId(userId, conversationId);

            if (optionalDocument.isPresent()) {
                ConversationDocument document = optionalDocument.get();
                // 仅处理活跃状态的对话
                if (document.isActive()) {
                    conversation = document.toConversation();
                    // 更新缓存
                    cacheService.cacheConversation(userId, conversation);
                }
            }
        }

        return conversation;
    }

    /**
     * 更新对话
     * @param userId
     * @param conversation
     * @return
     */
    public void updateConversation(String userId, Conversation conversation) {
        // 更新MongoDB
        Optional<ConversationDocument> optionalDocument =
                conversationRepository.findByUserIdAndConversationId(userId, conversation.getId());

        if (optionalDocument.isPresent()) {
            ConversationDocument document = optionalDocument.get();
            document.setMessages(conversation.getMessages());
            document.setLastUpdatedAt(conversation.getLastUpdatedAt());
            conversationRepository.save(document);
        } else {
            // 如果不存在则创建
            ConversationDocument document = ConversationDocument.fromConversation(conversation);
            conversationRepository.save(document);
        }

        // 更新缓存
        cacheService.cacheConversation(userId, conversation);
    }

    /**
     * 删除对话（逻辑删除）
     * @param userId
     * @param conversationId
     * @return
     */
    public void removeConversation(String userId, String conversationId) {
        // 从MongoDB中设置为非活跃
        Optional<ConversationDocument> optionalDocument =
                conversationRepository.findByUserIdAndConversationId(userId, conversationId);

        if (optionalDocument.isPresent()) {
            ConversationDocument document = optionalDocument.get();
            document.setActive(false);
            conversationRepository.save(document);
        }

        // 从缓存删除
        cacheService.removeConversation(userId, conversationId);
    }

    /**
     * 定时清理过期对话（每天执行一次）
     * 对话超过30天未更新则视为过期
     * @Scheduled 注解用于定时任务
     * cron表达式表示每天凌晨2点执行
     * @throws Exception
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanExpiredConversations() {
        log.info("开始清理过期对话");
        try {
            // 获取30天前的时间戳
            long expiryTimestamp = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();

            // 查找所有超过30天未更新的活跃对话
            conversationRepository.findByLastUpdatedAtBeforeAndActiveIsTrue(expiryTimestamp)
                .forEach(document -> {
                    document.setActive(false);
                    conversationRepository.save(document);
                    log.info("对话已过期: userId={}, conversationId={}",
                            document.getUserId(), document.getConversationId());
                });
        } catch (Exception e) {
            log.error("清理过期对话失败", e);
        }
    }
}
