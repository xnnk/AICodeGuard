package com.ai.aicodeguard.domain.codegen.service;

import com.ai.aicodeguard.domain.codegen.document.GeneratedCodeDocument;

/**
 * @ClassName: CodeGenerationService
 * @Description: 代码生成服务接口
 * @Author: LZX
 * @Date: 2025/4/20 00:28
 */
public interface CodeGenerationService {
    /**
     * 根据自然语言需求生成代码
     * @param prompt 自然语言需求
     * @param language 目标编程语言
     * @param userId 用户ID
     * @return 生成的代码对象
     */
    GeneratedCodeDocument generateCode(String prompt, String language, Integer userId, String modelType);
}
