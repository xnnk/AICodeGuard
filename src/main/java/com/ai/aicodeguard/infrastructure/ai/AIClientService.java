package com.ai.aicodeguard.infrastructure.ai;

import com.ai.aicodeguard.infrastructure.ai.conversation.Conversation;

/**
 * @InterfaceName: AIClientService
 * @Description: AI客户端服务接口
 * @Author: LZX
 * @Date: 2025/4/20 00:34
 */
public interface AIClientService {
    /**
     * 调用AI模型生成代码（单次）
     * @param prompt 自然语言需求
     * @param language 目标编程语言
     * @return 生成的代码内容
     */
    String generateCode(String prompt, String language);

    /**
     * 在对话中发送消息并获取回复
     * @param conversation 对话对象
     * @param message 发送的消息
     * @return AI模型的回复
     */
    String sendMessage(Conversation conversation, String message);

    /**
     * 创建新的对话
     * @param userId 用户ID
     * @return 新创建的对话对象
     */
    Conversation createConversation(String userId);

    /**
     * 获取支持的模型类型
     * @return 模型类型
     */
    AIModelType getModelType();
}
