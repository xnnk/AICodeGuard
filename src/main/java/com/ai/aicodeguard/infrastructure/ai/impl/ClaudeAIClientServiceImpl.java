package com.ai.aicodeguard.infrastructure.ai.impl;

import com.ai.aicodeguard.infrastructure.ai.AIClientService;
import com.ai.aicodeguard.infrastructure.ai.AIModelProperties;
import com.ai.aicodeguard.infrastructure.ai.AIModelType;
import com.ai.aicodeguard.infrastructure.ai.StreamingResponseHandler;
import com.ai.aicodeguard.infrastructure.ai.conversation.Conversation;
import com.ai.aicodeguard.infrastructure.ai.conversation.ConversationManager;
import com.ai.aicodeguard.infrastructure.ai.streaming.DefaultStreamingResponseHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @ClassName: ClaudeAIClientServiceImpl
 * @Description: Claude AI客户端服务实现类
 * @Author: LZX
 * @Date: 2025/4/27 13:47
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClaudeAIClientServiceImpl implements AIClientService {

    private final AIModelProperties properties;
    private final ConversationManager conversationManager;
    private final ObjectMapper objectMapper;

    /**
     * 生成代码
     * @param prompt 自然语言需求
     * @param language 目标编程语言
     * @return
     */
    @Override
    public String generateCode(String prompt, String language) {
        log.info("调用Claude模型生成{}代码", language);
        AIModelProperties.ModelConfig config = properties.getModel("claude");

        // 构建提示词
        String fullPrompt = buildPrompt(prompt, language);

        // 构建请求消息
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", fullPrompt);
        messages.add(message);

        try {
            // 发送非流式请求
            String responseContent = sendRequest(config, messages, false);

            // 提取代码块
            return extractCodeBlock(responseContent, language);
        } catch (Exception e) {
            log.error("调用Claude模型生成代码失败", e);
            throw new RuntimeException("调用Claude模型生成代码失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息到Claude模型
     * @param conversation 对话对象
     * @param message 发送的消息
     * @return
     */
    @Override
    public String sendMessage(Conversation conversation, String message) {
        log.info("在对话{}中发送消息", conversation.getId());
        AIModelProperties.ModelConfig config = properties.getModel("claude");

        // 添加用户消息到对话
        conversation.addUserMessage(message);

        // 构建Claude API消息格式
        List<Map<String, String>> messages = conversation.getMessages().stream()
                .map(msg -> {
                    Map<String, String> msgMap = new HashMap<>();
                    String role = "system".equals(msg.role()) ? "system" :
                            "user".equals(msg.role()) ? "user" : "assistant";
                    msgMap.put("role", role);
                    msgMap.put("content", msg.content());
                    return msgMap;
                })
                .collect(Collectors.toList());

        try {
            // 发送非流式请求
            String responseContent = sendRequest(config, messages, false);

            // 添加AI回复到对话
            conversation.addAssistantMessage(responseContent);

            // 更新对话
            conversationManager.updateConversation(conversation.getUserId(), conversation);

            return responseContent;
        } catch (Exception e) {
            log.error("在对话中调用Claude模型失败", e);
            throw new RuntimeException("在对话中调用Claude模型失败: " + e.getMessage());
        }
    }

    /**
     *
     * @param userId 用户ID
     * @return
     */
    @Override
    public Conversation createConversation(String userId) {
        return conversationManager.createConversation(userId, "claude");
    }


    /**
     * 获取模型类型
     * @return
     */
    @Override
    public AIModelType getModelType() {
        return AIModelType.CLAUDE;
    }

    /**
     * 发送流式消息到Claude模型
     * @param conversation 对话对象
     * @param message 发送的消息
     * @param emitter 服务器发送事件发射器
     * @param handler 流式响应处理器
     */
    @Override
    @SneakyThrows
    public void sendMessageStreaming(Conversation conversation, String message,
                                     SseEmitter emitter, StreamingResponseHandler handler) {
        log.info("在对话{}中发送流式消息", conversation.getId());
        AIModelProperties.ModelConfig config = properties.getModel("claude");
        AIModelProperties.ProxyConfig proxyConfig = properties.getProxy();

        // 添加用户消息到对话
        conversation.addUserMessage(message);

        // 构建Claude API消息格式
        List<Map<String, String>> messages = conversation.getMessages().stream()
                .map(msg -> {
                    Map<String, String> msgMap = new HashMap<>();
                    String role = "system".equals(msg.role()) ? "system" :
                            "user".equals(msg.role()) ? "user" : "assistant";
                    msgMap.put("role", role);
                    msgMap.put("content", msg.content());
                    return msgMap;
                })
                .collect(Collectors.toList());

        try {
            // 创建OkHttp客户端
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS);

            // 如果代理配置可用，添加代理
            if (proxyConfig != null && proxyConfig.isEnabled()) {
                log.info("使用代理 {}:{}", proxyConfig.getHost(), proxyConfig.getPort());
                clientBuilder.proxy(
                        new Proxy(
                                Proxy.Type.HTTP,
                                new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort())
                        )
                );
            }

            OkHttpClient client = clientBuilder.build();

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModelName());
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 4000);
            requestBody.put("temperature", 0.2);
            requestBody.put("stream", true); // 开启流式响应
            requestBody.put("system", "你是一个有用的AI助手，专注于帮助用户解决编程问题。");

            // 转换请求体为JSON
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 构建请求
            Request request = new Request.Builder()
                    .url(config.getEndpoint("conversation"))
                    .post(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")))
                    .addHeader("x-api-key", config.getApiKey())
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .build();

            // 发送请求并处理流式响应
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    log.error("Claude流式请求失败:", e);
                    handler.handleError(e, emitter);
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws java.io.IOException {
                    try (response) {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body().string();
                            log.error("Claude API返回错误: {}, {}", response.code(), errorBody);
                            handler.handleError(
                                    new RuntimeException("API调用失败，状态码: " + response.code()),
                                    emitter
                            );
                            return;
                        }

                        okhttp3.ResponseBody body = response.body();
                        if (body == null) {
                            handler.handleError(new RuntimeException("响应体为空"), emitter);
                            return;
                        }

                        StringBuilder completeMessage = new StringBuilder();
                        try (okio.BufferedSource source = body.source()) {
                            while (!source.exhausted()) {
                                // 读取一行数据
                                String line = source.readUtf8Line();
                                if (line == null) break;

                                // 解析Claude的SSE数据
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6);
                                    if ("[DONE]".equals(data.trim())) {
                                        // 流结束
                                        if (handler instanceof DefaultStreamingResponseHandler) {
                                            String finalMessage = completeMessage.toString();
                                            // 添加AI回复到对话
                                            conversation.addAssistantMessage(finalMessage);
                                            // 更新对话
                                            conversationManager.updateConversation(
                                                    conversation.getUserId(),
                                                    conversation
                                            );
                                        }
                                        emitter.complete();
                                        break;
                                    }

                                    try {
                                        Map<String, Object> eventData = objectMapper.readValue(data, Map.class);
                                        if (eventData.containsKey("type") && "content_block_delta".equals(eventData.get("type"))) {
                                            Map<String, Object> delta = (Map<String, Object>) eventData.get("delta");
                                            if (delta != null && delta.containsKey("text")) {
                                                String content = (String) delta.get("text");
                                                completeMessage.append(content);
                                                handler.handleResponse(content, emitter);
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.warn("解析Claude流式响应块失败: {}", data, e);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("处理Claude流式响应出错:", e);
                            handler.handleError(e, emitter);
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("创建Claude流式请求失败:", e);
            handler.handleError(e, emitter);
        }
    }

    /**
     * 发送非流式请求到Claude API
     * @param config 模型配置
     * @param messages 消息列表
     * @param isStream 是否流式请求
     * @return 响应内容
     */
    private String sendRequest(AIModelProperties.ModelConfig config, List<Map<String, String>> messages, boolean isStream) {
        AIModelProperties.ProxyConfig proxyConfig = properties.getProxy();
        String endpoint = config.getEndpoint("completion");

        try {
            // 创建OkHttp客户端
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS);

            // 如果代理配置可用，添加代理
            if (proxyConfig != null && proxyConfig.isEnabled()) {
                log.info("使用代理 {}:{}", proxyConfig.getHost(), proxyConfig.getPort());
                clientBuilder.proxy(
                        new Proxy(
                                Proxy.Type.HTTP,
                                new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort())
                        )
                );
            }

            OkHttpClient client = clientBuilder.build();

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModelName());
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 4000);
            requestBody.put("temperature", 0.2);
            if (isStream) {
                requestBody.put("stream", true);
            }

            // Claude API使用独立的system参数
            requestBody.put("system", "你是一个专业的编程助手，专注于生成干净、高效的代码。");

            // 转换请求体为JSON
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 构建请求
            Request request = new Request.Builder()
                    .url(endpoint)
                    .post(RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")))
                    .addHeader("x-api-key", config.getApiKey())
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .build();

            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    log.error("Claude API返回错误: {}, {}", response.code(), errorBody);
                    throw new RuntimeException("API调用失败，状态码: " + response.code());
                }

                String responseBody = response.body().string();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

                // 解析响应
                if (responseMap.containsKey("content")) {
                    List<Map<String, Object>> contentList = (List<Map<String, Object>>) responseMap.get("content");
                    if (!contentList.isEmpty()) {
                        StringBuilder contentBuilder = new StringBuilder();
                        for (Map<String, Object> contentItem : contentList) {
                            if ("text".equals(contentItem.get("type"))) {
                                contentBuilder.append(contentItem.get("text"));
                            }
                        }
                        return contentBuilder.toString();
                    }
                }

                log.error("无法从Claude响应中解析内容");
                throw new RuntimeException("无法从Claude响应中解析内容");
            }
        } catch (Exception e) {
            log.error("调用Claude API失败", e);
            throw new RuntimeException("调用Claude API失败: " + e.getMessage());
        }
    }

    /**
     * 解析Claude流式响应块
     */
    private String parseClaudeStreamChunk(String chunk) {
        // Claude的流格式为: data: {...JSON数据...}
        if (chunk.startsWith("data: ")) {
            chunk = chunk.substring(6);

            // 判断是否是流结束标记
            if ("[DONE]".equals(chunk.trim())) {
                return "[DONE]";
            }

            try {
                // 解析JSON
                Map<String, Object> response = objectMapper.readValue(chunk, Map.class);

                // Claude的流式响应格式可能与其他不同，需要适配
                if (response.containsKey("delta") && response.get("delta") instanceof Map) {
                    Map<String, Object> delta = (Map<String, Object>) response.get("delta");
                    if (delta.containsKey("text")) {
                        return (String) delta.get("text");
                    }
                }

                // 处理可能的type/text格式
                if (response.containsKey("type") && "content_block_delta".equals(response.get("type"))) {
                    Map<String, Object> delta = (Map<String, Object>) response.get("delta");
                    if (delta != null && delta.containsKey("text")) {
                        return (String) delta.get("text");
                    }
                }
            } catch (Exception e) {
                log.warn("解析Claude流式响应块失败: {}", chunk, e);
            }
        }
        return "";
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(String prompt, String language) {
        return String.format(
                """
                我需要你帮我生成%s代码。以下是需求描述：
                
                %s
                
                请生成符合需求的完整代码，不要包含额外的解释。代码需要遵循最佳实践，
                保证安全性、可读性和性能。使用适当的注释说明关键步骤。""",
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
