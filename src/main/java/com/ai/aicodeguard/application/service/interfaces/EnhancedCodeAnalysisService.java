package com.ai.aicodeguard.application.service.interfaces;

import com.ai.aicodeguard.presentation.response.codegen.EnhancedCodeAnalysisResult;
import com.ai.aicodeguard.presentation.request.codegen.CodeGenerationRequest;

/**
 * @ClassName: EnhancedCodeAnalysisService
 * @Description: 增强代码分析服务接口
 * @Author: LZX
 * @Date: 2025/5/10 12:10
 */
public interface EnhancedCodeAnalysisService {
    /**
     * 生成代码，并利用知识图谱进行增强分析
     * @param request 代码生成请求
     * @param userId 用户ID
     * @return 增强分析结果
     */
    EnhancedCodeAnalysisResult generateCodeAndAnalyzeWithKnowledgeGraph(CodeGenerationRequest request, Integer userId);
}
