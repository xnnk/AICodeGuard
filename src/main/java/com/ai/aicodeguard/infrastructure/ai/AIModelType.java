package com.ai.aicodeguard.infrastructure.ai;

/**
 * @ClassName: AIModelType
 * @Description: AI模型类型枚举
 * @Author: LZX
 * @Date: 2025/4/20 01:23
 */
public enum AIModelType {
    DEEPSEEK,
    OPENAI,
    CLAUDE,
    GEMINI;

    public static AIModelType fromString(String type) {
        try {
            return valueOf(type.toUpperCase());
        } catch (Exception e) {
            return DEEPSEEK;
        }
    }
}
