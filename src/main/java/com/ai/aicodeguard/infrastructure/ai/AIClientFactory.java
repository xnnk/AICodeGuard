package com.ai.aicodeguard.infrastructure.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: AIClientFactory
 * @Description: AI模型工厂
 * @Author: LZX
 * @Date: 2025/4/20 01:24
 */
@Component
public class AIClientFactory {

    private final Map<AIModelType, AIClientService> clientMap = new HashMap<>();
    private final AIModelProperties properties;

    @Autowired
    public AIClientFactory(List<AIClientService> clients, AIModelProperties properties) {
        this.properties = properties;
        for (AIClientService client : clients) {
            clientMap.put(client.getModelType(), client);
        }
    }

    /**
     * 获取指定类型的AI客户端
     */
    public AIClientService getClient(String modelType) {
        AIModelType type = modelType != null ?
                AIModelType.fromString(modelType) :
                AIModelType.fromString(properties.getDefaultModel().getModelName());

        AIClientService client = clientMap.get(type);
        if (client == null) {
            throw new IllegalArgumentException("不支持的AI模型类型: " + type);
        }
        return client;
    }

    /**
     * 获取默认的AI客户端
     */
    public AIClientService getDefaultClient() {
        return getClient(properties.getDefaultModel().getModelName());
    }
}
