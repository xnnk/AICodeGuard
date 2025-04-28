package com.ai.aicodeguard.domain.codegen.service.impl;

import com.ai.aicodeguard.domain.codegen.GeneratedCode;
import com.ai.aicodeguard.domain.codegen.document.GeneratedCodeDocument;
import com.ai.aicodeguard.domain.codegen.service.CodeGenerationService;
import com.ai.aicodeguard.infrastructure.ai.AIClientFactory;
import com.ai.aicodeguard.infrastructure.ai.AIClientService;
import com.ai.aicodeguard.infrastructure.mongo.GeneratedCodeDocumentRepository;
import com.ai.aicodeguard.infrastructure.persistence.GeneratedCodeRepository;
import com.ai.aicodeguard.infrastructure.security.SecurityScanningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
/**
 * @ClassName: CodeGenerationServiceImpl
 * @Description: 代码生成服务实现类
 * @Author: LZX
 * @Date: 2025/4/20 00:29
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CodeGenerationServiceImpl implements CodeGenerationService {

    private final AIClientFactory aiClientFactory;
    private final GeneratedCodeRepository generatedCodeRepository;
    private final GeneratedCodeDocumentRepository generatedCodeDocumentRepository;
    private final SecurityScanningService securityScanningService; // 注入安全扫描服务

    @Transactional
    public GeneratedCodeDocument generateCode(String prompt, String language, Integer userId) {
        return generateCode(prompt, language, userId, null);
    }

    /**
     * 根据自然语言需求生成代码，并指定AI模型
     * @param prompt    自然语言需求
     * @param language  目标编程语言
     * @param userId    用户ID
     * @param modelType AI模型类型
     * @return 生成的代码对象
     */
    @Transactional
    public GeneratedCodeDocument generateCode(String prompt, String language, Integer userId, String modelType) {
        log.info("开始为用户{}使用{}模型生成{}代码，需求: {}", userId, modelType != null ? modelType : "默认", language, prompt);

        // 1. 获取AI客户端并调用生成代码
        AIClientService aiClient = modelType != null ?
            aiClientFactory.getClient(modelType) :
            aiClientFactory.getDefaultClient();

        String generatedContent = aiClient.generateCode(prompt, language);
        String codeId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        String usedModelType = aiClient.getModelType().name().toLowerCase();

        // 2. 保存代码元数据到MySQL
        GeneratedCode codeMetadata = new GeneratedCode();
        codeMetadata.setId(codeId);
        codeMetadata.setUserId(userId);
        codeMetadata.setPrompt(prompt);
        codeMetadata.setLanguage(language);
        codeMetadata.setCreatedAt(now);
        codeMetadata.setAiModel(usedModelType); // 记录实际使用的模型
        codeMetadata.setScanStatus(GeneratedCode.ScanStatus.PENDING);

        generatedCodeRepository.save(codeMetadata);

        // 3. 保存代码内容到MongoDB
        GeneratedCodeDocument codeDocument = new GeneratedCodeDocument();
        codeDocument.setId(codeId);
        codeDocument.setContent(generatedContent);
        codeDocument.setLanguage(language);
        codeDocument.setPrompt(prompt);
        codeDocument.setAiModel(usedModelType);
        codeDocument.setUserId(userId);
        codeDocument.setCreatedAt(now);
        codeDocument.setScanStatus(GeneratedCode.ScanStatus.PENDING.name());

        generatedCodeDocumentRepository.save(codeDocument);

        // 触发安全扫描
        securityScanningService.scanGeneratedCode(codeId);

        log.info("代码生成完成，代码ID: {}, 使用模型: {}", codeId, usedModelType);
        return codeDocument;
    }
}
