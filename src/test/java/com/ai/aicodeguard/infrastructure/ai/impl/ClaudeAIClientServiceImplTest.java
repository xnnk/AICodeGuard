package com.ai.aicodeguard.infrastructure.ai.impl;

import com.ai.aicodeguard.infrastructure.ai.StreamingResponseHandler;
import com.ai.aicodeguard.infrastructure.ai.conversation.Conversation;
import com.ai.aicodeguard.infrastructure.ai.impl.ClaudeAIClientServiceImpl;
import com.ai.aicodeguard.infrastructure.ai.streaming.DefaultStreamingResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Disabled("需要有效的API密钥，仅在本地测试")
public class ClaudeAIClientServiceImplTest {

    @Autowired
    private ClaudeAIClientServiceImpl claudeAIClient;

    private Conversation conversation;

    @BeforeEach
    public void setup() {
        conversation = claudeAIClient.createConversation("1");
    }

    @Test
    public void testGenerateCode() {
        String prompt = "实现一个简单的Java计算器，支持加减乘除基本运算";
        String language = "Java";

        String result = claudeAIClient.generateCode(prompt, language);

        System.out.println(claudeAIClient.getModelType());

        assertNotNull(result);
        assertTrue(result.length() > 0);
        // 内容应包含计算器相关的类或方法
        assertTrue(result.contains("class") || result.contains("public") || result.contains("Calculator"));
        System.out.println("生成的代码: " + result);
    }

    @Test
    public void testSendMessage() {
        String message = "你好，请介绍一下Java中的Stream API";

        String response = claudeAIClient.sendMessage(conversation, message);

        assertNotNull(response);
        assertTrue(response.length() > 0);
        // 应该包含关于Stream API的信息
        assertTrue(response.contains("Stream") || response.contains("流") || response.contains("Java 8"));
        System.out.println("AI回复: " + response);
    }

    @Test
    public void testSendMessageStreaming() throws Exception {
        String message = "请简单介绍一下Spring框架";
        SseEmitter emitter = new SseEmitter(30000L); // 30秒超时
        CountDownLatch latch = new CountDownLatch(1);
        final StringBuilder responseBuilder = new StringBuilder();

        StreamingResponseHandler handler = new DefaultStreamingResponseHandler() {
            @Override
            public boolean handleResponse(String response, SseEmitter emitter) {
                if ("[DONE]".equals(response)) {
                    latch.countDown();
                    return true;
                }
                responseBuilder.append(response);
                return false;
            }
        };

        // 执行流式响应
        claudeAIClient.sendMessageStreaming(conversation, message, emitter, handler);

        // 等待响应完成
        boolean completed = latch.await(30, TimeUnit.SECONDS);

        assertTrue(completed, "响应应在超时前完成");
        String fullResponse = responseBuilder.toString();
        assertNotNull(fullResponse);
        assertTrue(fullResponse.length() > 0);
        // 应包含Spring相关信息
        assertTrue(fullResponse.contains("Spring") || fullResponse.contains("框架") || fullResponse.contains("依赖注入"));

        System.out.println("流式AI回复: " + fullResponse);
    }
}