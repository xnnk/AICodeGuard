package com.ai.aicodeguard.infrastructure.ai.impl;

import com.ai.aicodeguard.infrastructure.ai.AIClientService;
import com.ai.aicodeguard.infrastructure.ai.AIModelProperties;
import com.ai.aicodeguard.infrastructure.ai.AIModelType;
import com.ai.aicodeguard.infrastructure.ai.conversation.Conversation;
import com.ai.aicodeguard.infrastructure.ai.conversation.ConversationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName: DeepseekAIClientServiceImpl
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/20 00:36
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeepseekAIClientServiceImpl implements AIClientService {

    private final AIModelProperties properties;

    private final RestTemplate restTemplate;

    private final ConversationManager conversationManager;


    @Override
    public String generateCode(String prompt, String language) {
        log.info("调用DeepSeek模型生成{}代码", language);
        AIModelProperties.ModelConfig config = properties.getModel("deepseek");

        // 使用completion端点
        String endpoint = config.getEndpoint("completion");

        // 构建提示词
        String fullPrompt = buildPrompt(prompt, language);

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getApiKey());

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModelName());

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", fullPrompt);

        requestBody.put("messages", List.of(message));
        requestBody.put("temperature", 0.2);  // 较低的温度以获得更确定的输出
        requestBody.put("max_tokens", 4000);  // 根据需要调整

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            // 发送请求
            Map<String, Object> response = restTemplate.postForObject(
                endpoint,
                request,
                Map.class
            );

            // 解析响应
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message1 = (Map<String, Object>) choice.get("message");
                    String content = (String) message1.get("content");

                    // 提取代码块
                    return extractCodeBlock(content, language);
                }
            }

            log.error("无法从DeepSeek响应中解析代码");
            throw new RuntimeException("无法从DeepSeek响应中解析代码");

        } catch (Exception e) {
            log.error("调用DeepSeek模型生成代码失败", e);
            throw new RuntimeException("调用DeepSeek模型生成代码失败: " + e.getMessage());
        }
    }

    @Override
    public Conversation createConversation(String userId) {
        return conversationManager.createConversation(userId, "deepseek");
    }

    @Override
    public String sendMessage(Conversation conversation, String message) {
        log.info("在对话{}中发送消息", conversation.getId());
        AIModelProperties.ModelConfig config = properties.getModel("deepseek");

        // 使用conversation端点
        String endpoint = config.getEndpoint("conversation");

        // 添加用户消息到对话
        conversation.addUserMessage(message);

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getApiKey());

        // 构建消息列表
        List<Map<String, String>> messages = conversation.getMessages().stream()
            .map(msg -> {
                Map<String, String> msgMap = new HashMap<>();
                msgMap.put("role", msg.role());
                msgMap.put("content", msg.content());
                return msgMap;
            })
            .collect(Collectors.toList());

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModelName());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.5);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            // 发送请求
            Map<String, Object> response = restTemplate.postForObject(
                endpoint,
                request,
                Map.class
            );

            // 解析响应
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> msgObj = (Map<String, Object>) choice.get("message");
                    String content = (String) msgObj.get("content");

                    // 添加AI回复到对话
                    conversation.addAssistantMessage(content);

                    // 更新对话
                    conversationManager.updateConversation(conversation.getUserId(), conversation);

                    return content;
                }
            }

            String errorMsg = "无法从DeepSeek响应中解析回复";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);

        } catch (Exception e) {
            log.error("在对话中调用DeepSeek模型失败", e);
            throw new RuntimeException("在对话中调用DeepSeek模型失败: " + e.getMessage());
        }
    }

    @Override
    public AIModelType getModelType() {
        return AIModelType.DEEPSEEK;
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(String prompt, String language) {
        return String.format(
            "我需要你帮我生成%s代码。以下是需求描述：\n\n%s\n\n" +
                    "请生成符合需求的完整代码，不要包含额外的解释。代码需要遵循最佳实践，" +
                    "保证安全性、可读性和性能。使用适当的注释说明关键步骤。",
            language, prompt
        );
    }

    /**
     * 从AI响应中提取代码块
     */
    private String extractCodeBlock(String content, String language) {
        // 检查内容是否包含Markdown代码块
        String codeBlockStart = "```" + language.toLowerCase();
        String codeBlockEnd = "```";

        int startIndex = content.indexOf(codeBlockStart);
        if (startIndex != -1) {
            startIndex = content.indexOf('\n', startIndex) + 1;
            int endIndex = content.indexOf(codeBlockEnd, startIndex);
            if (endIndex != -1) {
                return content.substring(startIndex, endIndex).trim();
            }
        }

        // 无法识别代码块，返回整个内容
        return content.trim();
    }
}
