package com.ai.aicodeguard.presentation.response.codegen;

import com.ai.aicodeguard.domain.codegen.document.VulnerabilityReport;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @ClassName: EnhancedCodeAnalysisResult
 * @Description: 封装代码生成及知识图谱增强分析的最终结果
 * @Author: LZX
 * @Date: 2025-05-10 12:00
 */
@Data
@Builder
public class EnhancedCodeAnalysisResult {
    private String generatedCodeId;
    private String generatedCodeContent;
    private String language;
    private String modelUsedForGeneration;
    private List<String> knowledgeGraphCypherQueries; // LLM 生成的 Cypher 查询语句
    private List<Map<String, Object>> knowledgeGraphDataRetrieved; // 从知识图谱检索到的数据
    private VulnerabilityReport analysisReport; // 增强后的分析报告
    private String modelUsedForAnalysis; // 用于最终分析的模型
}