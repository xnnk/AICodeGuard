package com.ai.aicodeguard.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @ClassName: RedisConfig
 * @Description: Redis配置类
 * @Author: LZX
 * @Date: 2025/4/24 17:51
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate配置
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate实例
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());         // 设置key序列化器
        template.setValueSerializer(new StringRedisSerializer());       // 设置value序列化器
        template.setHashKeySerializer(new StringRedisSerializer());     // 设置hash key序列化器
        template.setHashValueSerializer(new StringRedisSerializer());   // 设置hash value序列化器
        template.afterPropertiesSet();                                  // 初始化RedisTemplate
        return template;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();                                      // 创建ObjectMapper实例
    }
}