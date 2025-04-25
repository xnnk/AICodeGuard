package com.ai.aicodeguard.infrastructure.cache;

import com.ai.aicodeguard.infrastructure.ai.conversation.Conversation;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @ClassName: ConversationCacheService
 * @Description: Redis缓存服务，用于存储对话记录
 * @Author: LZX
 * @Date: 2025/4/24 17:39
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "conversation:";
    private static final long CACHE_TTL_HOURS = 24; // 缓存24小时

    private String buildKey(String userId, String conversationId) {
        return CACHE_KEY_PREFIX + userId + ":" + conversationId;
    }

    public void cacheConversation(String userId, Conversation conversation) {
        try {
            String key = buildKey(userId, conversation.getId());
            String json = objectMapper.writeValueAsString(conversation);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("缓存对话失败", e);
        }
    }

    public Conversation getConversation(String userId, String conversationId) {
        try {
            String key = buildKey(userId, conversationId);
            String json = redisTemplate.opsForValue().get(key);

            if (json != null) {
                // 每次访问，刷新过期时间
                redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);
                return objectMapper.readValue(json, Conversation.class);
            }
        } catch (Exception e) {
            log.error("获取缓存对话失败", e);
        }
        return null;
    }

    public void removeConversation(String userId, String conversationId) {
        try {
            String key = buildKey(userId, conversationId);
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("删除缓存对话失败", e);
        }
    }
}
