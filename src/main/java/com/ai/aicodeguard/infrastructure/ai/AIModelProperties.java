package com.ai.aicodeguard.infrastructure.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: AIModelProperties
 * @Description: AI模型配置
 * @Author: LZX
 * @Date: 2025/4/20 00:33
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AIModelProperties {

    private Map<String, ModelConfig> models = new HashMap<>();
    private String defaultModel;

    @Data
    public static class ModelConfig {
        private String apiKey;
        // 修改为Map类型支持多种端点
        private Map<String, String> endpoints = new HashMap<>();
        private int timeout = 30000;
        private String modelName;

        /**
         * 获取指定类型的端点
         */
        public String getEndpoint(String type) {
            return endpoints.getOrDefault(type, endpoints.get("completion"));
        }

        /**
         * 获取默认端点(完成型端点)
         */
        public String getDefaultEndpoint() {
            return endpoints.getOrDefault("completion", null);
        }
    }

    /**
     * 获取指定模型的配置
     */
    public ModelConfig getModel(String modelName) {
        return models.get(modelName != null ? modelName : defaultModel);
    }

    /**
     * 获取默认模型的配置
     */
    public ModelConfig getDefaultModel() {
        return models.get(defaultModel);
    }
}
