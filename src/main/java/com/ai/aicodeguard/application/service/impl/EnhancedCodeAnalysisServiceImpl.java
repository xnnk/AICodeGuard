package com.ai.aicodeguard.application.service.impl;

import com.ai.aicodeguard.presentation.response.codegen.EnhancedCodeAnalysisResult;
import com.ai.aicodeguard.application.service.interfaces.EnhancedCodeAnalysisService;
import com.ai.aicodeguard.domain.codegen.GeneratedCode;
import com.ai.aicodeguard.domain.codegen.document.GeneratedCodeDocument;
import com.ai.aicodeguard.domain.codegen.document.VulnerabilityReport;
import com.ai.aicodeguard.infrastructure.ai.AIClientFactory;
import com.ai.aicodeguard.infrastructure.ai.AIClientService;
import com.ai.aicodeguard.infrastructure.graph.KnowledgeGraphService;
import com.ai.aicodeguard.infrastructure.mongo.GeneratedCodeDocumentRepository;
import com.ai.aicodeguard.infrastructure.persistence.GeneratedCodeRepository;
import com.ai.aicodeguard.presentation.request.codegen.CodeGenerationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @ClassName: EnhancedCodeAnalysisServiceImpl
 * @Description: 实现代码生成并结合知识图谱进行增强分析的服务
 * @Author: LZX
 * @Date: 2025/5/10 15:43
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedCodeAnalysisServiceImpl implements EnhancedCodeAnalysisService {

    private final AIClientFactory aiClientFactory;
    private final GeneratedCodeRepository generatedCodeRepository;
    private final GeneratedCodeDocumentRepository generatedCodeDocumentRepository;
    private final KnowledgeGraphService knowledgeGraphService;
    private final ObjectMapper objectMapper; // 用于解析LLM返回的JSON

    // 定义提取Cypher代码块的正则表达式
    private static final Pattern CYPHER_BLOCK_PATTERN = Pattern.compile("```cypher\\s*([\\s\\S]*?)\\s*```", Pattern.MULTILINE);


    @Override
    @Transactional
    public EnhancedCodeAnalysisResult generateCodeAndAnalyzeWithKnowledgeGraph(CodeGenerationRequest request, Integer userId) {
        long startTime = System.currentTimeMillis();
        log.info("开始为用户 {} 生成 {} 代码并进行增强分析，需求: {}", userId, request.getLanguage(), request.getPrompt());

        // 1: 初步代码生成
        AIClientService codeGenerationClient = aiClientFactory.getClient(request.getModelType()); // 或者选择特定的代码生成模型
        String initialGeneratedCodeContent = codeGenerationClient.generateCode(request.getPrompt(), request.getLanguage());
        String modelUsedForGeneration = codeGenerationClient.getModelType().name();
        log.info("初步代码生成完成，使用模型: {}", modelUsedForGeneration);

        // 保存初步生成的代码元数据和文档 (与 CodeGenerationServiceImpl类似)
        String codeId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        GeneratedCode codeMetadata = new GeneratedCode();
        codeMetadata.setId(codeId);
        codeMetadata.setUserId(userId);
        codeMetadata.setPrompt(request.getPrompt());
        codeMetadata.setLanguage(request.getLanguage());
        codeMetadata.setCreatedAt(now);
        codeMetadata.setAiModel(modelUsedForGeneration);
        codeMetadata.setScanStatus(GeneratedCode.ScanStatus.PENDING); // 初始状态
        generatedCodeRepository.save(codeMetadata);

        GeneratedCodeDocument codeDocument = new GeneratedCodeDocument();
        codeDocument.setId(codeId);
        codeDocument.setContent(initialGeneratedCodeContent);
        codeDocument.setLanguage(request.getLanguage());
        codeDocument.setPrompt(request.getPrompt());
        codeDocument.setAiModel(modelUsedForGeneration);
        codeDocument.setUserId(userId);
        codeDocument.setCreatedAt(now);
        codeDocument.setScanStatus(GeneratedCode.ScanStatus.PENDING.name());
        codeDocument.setIsVisible(true); // 默认为可见
        generatedCodeDocumentRepository.save(codeDocument);


        // 2: LLM 决策并生成 Cypher 查询
        log.info("代码 {}：开始让 LLM 生成知识图谱查询语句", codeId);
        AIClientService cypherGenerationClient = aiClientFactory.getClient("claude"); // 假设使用 Claude 进行Cypher生成，因为它在处理复杂指令和结构化输出方面可能表现较好
        String knowledgeGraphSchema = getKnowledgeGraphSchemaDescription(); // 获取图谱 Schema 描述
        String cypherGenPrompt = buildCypherGenerationPrompt(initialGeneratedCodeContent, request.getLanguage(), knowledgeGraphSchema);

        String llmGeneratedCypherResponse = cypherGenerationClient.generateCode(cypherGenPrompt, "cypher"); // 要求LLM输出Cypher
        List<String> cypherQueries = extractCypherQueriesFromLlmResponse(llmGeneratedCypherResponse);
        log.info("代码 {}：LLM 生成了 {} 条 Cypher 查询语句", codeId, cypherQueries.size());
        if (log.isDebugEnabled()) {
            cypherQueries.forEach(q -> log.debug("Generated Cypher: {}", q));
        }


        // 3: 执行知识图谱查询
        List<Map<String, Object>> knowledgeGraphData = new ArrayList<>();
        if (!cypherQueries.isEmpty()) {
            log.info("代码 {}：开始执行知识图谱查询", codeId);
            for (String cypherQuery : cypherQueries) {
                if (isValidReadOnlyCypher(cypherQuery)) { // 安全校验
                    try {
                        List<Map<String, Object>> queryResult = knowledgeGraphService.executeLlmLedReadOnlyQuery(cypherQuery);
                        if (queryResult != null && !queryResult.isEmpty()) {
                            knowledgeGraphData.addAll(queryResult);
                        }
                        log.debug("代码 {}：执行 Cypher 查询成功: {}", codeId, cypherQuery);
                    } catch (Exception e) {
                        log.warn("代码 {}：执行 Cypher 查询失败: {} - 错误: {}", codeId, cypherQuery, e.getMessage());
                        // 可以选择记录错误，或者部分成功
                    }
                } else {
                    log.warn("代码 {}：跳过无效或非只读的 Cypher 查询: {}", codeId, cypherQuery);
                }
            }
            log.info("代码 {}：知识图谱查询完成，检索到 {} 条相关数据记录", codeId, knowledgeGraphData.size());
        } else {
            log.info("代码 {}：LLM 未生成有效的 Cypher 查询语句，跳过知识图谱查询步骤", codeId);
        }


        // 4: 结合知识图谱进行代码分析
        log.info("代码 {}：开始结合知识图谱数据进行最终代码分析", codeId);
        String modelUsedForAnalysis = null;
        VulnerabilityReport analysisReport = null;
        try {
            AIClientService analysisClient = aiClientFactory.getClient("claude"); // 同样，Claude 可能适合复杂的分析任务
            String analysisPrompt = buildFinalAnalysisPrompt(initialGeneratedCodeContent, request.getLanguage(), knowledgeGraphData);
            modelUsedForAnalysis = analysisClient.getModelType().name();

            String llmAnalysisResponse = analysisClient.generateCode(analysisPrompt, "json"); // 要求LLM输出JSON格式的分析报告

            analysisReport = parseVulnerabilityReportFromLlmResponse(llmAnalysisResponse, codeId);
            log.info("代码 {}：最终代码分析完成，使用模型: {}", codeId, modelUsedForAnalysis);
        } catch (Exception e) {
            codeMetadata.setScanStatus(GeneratedCode.ScanStatus.FAILED);
            codeMetadata.setScanTime(LocalDateTime.now());
            generatedCodeRepository.save(codeMetadata);
            codeDocument.setScanStatus(GeneratedCode.ScanStatus.FAILED.name());
            generatedCodeDocumentRepository.save(codeDocument);
            throw new RuntimeException(e);
        }

        // 更新代码元数据的扫描状态
        codeMetadata.setScanStatus(GeneratedCode.ScanStatus.SUCCESS);
        codeMetadata.setScanTime(LocalDateTime.now());
        generatedCodeRepository.save(codeMetadata);
        codeDocument.setScanStatus(GeneratedCode.ScanStatus.SUCCESS.name());
        generatedCodeDocumentRepository.save(codeDocument);


        long endTime = System.currentTimeMillis();
        log.info("代码 {}：增强分析流程总耗时: {} ms", codeId, (endTime - startTime));

        return EnhancedCodeAnalysisResult.builder()
                .generatedCodeId(codeId)
                .generatedCodeContent(initialGeneratedCodeContent)
                .language(request.getLanguage())
                .modelUsedForGeneration(modelUsedForGeneration)
                .knowledgeGraphCypherQueries(cypherQueries)
                .knowledgeGraphDataRetrieved(knowledgeGraphData)
                .analysisReport(analysisReport)
                .modelUsedForAnalysis(modelUsedForAnalysis)
                .build();
    }

    /**
     * 构建用于让 LLM 生成 Cypher 查询的 Prompt。
     *
     * @param generatedCode      初步生成的代码
     * @param language           代码语言
     * @param kgSchemaDescription 知识图谱的 Schema 描述
     * @return Prompt 字符串
     */
    private String buildCypherGenerationPrompt(String generatedCode, String language, String kgSchemaDescription) {
        // 注意
        return String.format("""
            You are tasked with analyzing a piece of code and determining what information needs to be queried from a knowledge graph to assist in a detailed code evaluation, particularly focusing on security vulnerabilities and best practices. Your goal is to generate a set of Cypher query statements to retrieve this useful information based on the provided knowledge graph schema.
            
            Here is the knowledge graph schema description:
            <schema>
            {{%s}}
            </schema>
            
            Now, analyze the following {{%s}} code:
            <code>
            {{%s}}
            </code>
            
            To complete this task:
            
            1. Carefully examine the code for potential security issues, best practices, and any notable patterns or technologies used.
            
            2. Based on your analysis, determine what information would be helpful to query from the knowledge graph to assist in a more detailed evaluation.
            
            3. Generate Cypher query statements to retrieve this information. Each query should be:
               - Independent and complete
               - Read-only
               - Focused on retrieving data related to potential issues, technologies, or patterns in the code
            
            4.  Specific Instructions for Querying `Vulnerability` Nodes:
                When you need to query `Vulnerability` nodes, you **must** use one or more of the following properties in your `WHERE` clause: `cweId`, `severity`, or `name`. Adhere to these specific rules:
                * When using `cweId`: The "CWE" part must be uppercase (e.g., `MATCH (v:Vulnerability) WHERE v.cweId = 'CWE-79' RETURN v`). If querying for multiple `cweId`s, you can use the `IN` operator (e.g., `MATCH (v:Vulnerability) WHERE v.cweId IN ['CWE-79', 'CWE-89'] RETURN v`).
                * When using `severity`: The value(s) must be from the enumeration {CRITICAL, HIGH, MEDIUM, LOW}. If using a single value, use equality (e.g., `MATCH (v:Vulnerability) WHERE v.severity = 'HIGH' RETURN v`). If using multiple values, use the `IN` operator (e.g., `MATCH (v:Vulnerability) WHERE v.severity IN ['HIGH', 'CRITICAL'] RETURN v`).
                * When using `name`: You **must** use both the English and Chinese names of the vulnerability if you are targeting a specific known vulnerability by name, using the `IN` operator (e.g., `MATCH (v:Vulnerability) WHERE v.name IN ['SQL Injection', 'SQL注入'] RETURN v`).
            
            5. Wrap each Cypher query in triple backticks with the 'cypher' language specifier, like this:
               ```cypher
               YOUR_QUERY_HERE
               ```
            
            6. If you determine that no queries are necessary based on your analysis, do not output any Cypher code blocks.
            
            
            Remember:
            - Focus on queries that will provide valuable insights for security vulnerability assessment and best practice evaluation.
            - Ensure your queries align with the provided knowledge graph schema.
            - Do not modify the schema or assume the existence of nodes or relationships not specified in the schema.
            
            Your final output should consist only of the necessary Cypher query statements, each in its own code block. Do not include any explanations or additional text outside of the Cypher code blocks.
            """, kgSchemaDescription, language, generatedCode);
    }

    /**
     * 从 LLM 的响应中提取 Cypher 查询语句。
     * LLM 可能返回一个或多个用 ```cypher ... ``` 包裹的查询。
     */
    private List<String> extractCypherQueriesFromLlmResponse(String llmResponse) {
        if (!StringUtils.hasText(llmResponse)) {
            return Collections.emptyList();
        }
        List<String> queries = new ArrayList<>();
        Matcher matcher = CYPHER_BLOCK_PATTERN.matcher(llmResponse);
        while (matcher.find()) {
            String query = matcher.group(1).trim();
            if (StringUtils.hasText(query)) {
                queries.add(query);
            }
        }
        // 如果没有找到代码块，但响应本身可能就是一条Cypher语句
        if (queries.isEmpty() && llmResponse.toUpperCase().contains("MATCH") && !llmResponse.contains("```")) {
            // 尝试将整个响应视为单个查询，但这可能不准确，需要谨慎处理
            String cleanedResponse = llmResponse.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.toLowerCase().startsWith("cypher")) // 移除可能的"cypher"前缀
                    .collect(Collectors.joining("\n"));
            if (StringUtils.hasText(cleanedResponse)) {
                queries.add(cleanedResponse);
                log.warn("No explicit Cypher block found, but response seems to contain a Cypher query. Extracted: {}", cleanedResponse);
            }
        }
        return queries;
    }


    /**
     * 校验 Cypher 查询是否为只读且基本有效
     */
    private boolean isValidReadOnlyCypher(String cypherQuery) {
        if (!StringUtils.hasText(cypherQuery)) {
            return false;
        }
        String upperCaseQuery = cypherQuery.toUpperCase();
        // 禁止写入操作
        if (upperCaseQuery.contains("CREATE ") || upperCaseQuery.contains("DELETE ") ||
                upperCaseQuery.contains("SET ") || upperCaseQuery.contains("REMOVE ") ||
                upperCaseQuery.contains("MERGE ")) { // MERGE 也可以用于创建，需谨慎
            return false;
        }
        // 必须包含 MATCH
        return upperCaseQuery.contains("MATCH ");
    }


    /**
     * 构建用于最终代码分析的 Prompt。
     *
     * @param generatedCode      初步生成的代码
     * @param language           代码语言
     * @param kgData             从知识图谱检索到的数据
     * @return Prompt 字符串
     */
    private String buildFinalAnalysisPrompt(String generatedCode, String language, List<Map<String, Object>> kgData) {
        String kgDataJson;
        try {
            kgDataJson = objectMapper.writeValueAsString(kgData);
        } catch (JsonProcessingException e) {
            log.warn("序列化知识图谱数据为JSON失败: {}", e.getMessage());
            kgDataJson = "[]"; // 提供空数组作为回退
        }

        // 注意
        return String.format("""
            You are a security expert tasked with analyzing code for vulnerabilities and best practices. You will be provided with code in a specific programming language, along with relevant information from a knowledge graph to assist in your analysis. Your task is to produce a comprehensive security vulnerability analysis and best practices assessment.
            
            The input variables are as follows:
            <language>{{%s}}</language>
            <knowledge_graph>
            {{%s}}
            </knowledge_graph>
            <code>
            {{%s}}
            </code>
            
            Your analysis should be output in a JSON format that conforms to the following VulnerabilityReport structure:
            
            {
              "vulnerabilities": [
                {
                  "type": "Vulnerability type name (e.g., CWE-ID)",
                  "description": "Detailed description of the vulnerability, incorporating knowledge graph information where relevant.",
                  "line": "Relevant code line number (integer)",
                  "column": "Relevant code column number (integer, optional)",
                  "codeSnippet": "Relevant code snippet",
                  "suggestion": "Remediation suggestion, incorporating fix patterns from the knowledge graph where applicable.",
                  "severity": "CRITICAL/HIGH/MEDIUM/LOW"
                }
              ],
              "summary": "Overall summary of the code's security and quality, explaining how the knowledge graph information assisted in this assessment.",
              "securityScore": "Code security score (0-100, integer, optional)"
            }
            
            To complete this task:
            
            1. Carefully review the provided code and the knowledge graph information.
            2. Identify any security vulnerabilities or best practice violations in the code.
            3. For each vulnerability or violation:
               a. Determine its type, referring to standard vulnerability classifications (e.g., CWE) where possible.
               b. Write a detailed description, incorporating relevant information from the knowledge graph.
               c. Identify the specific line (and column if applicable) where the issue occurs.
               d. Extract the relevant code snippet.
               e. Provide a suggestion for fixing the issue, using fix patterns from the knowledge graph if available.
               f. Assess the severity of the issue (CRITICAL, HIGH, MEDIUM, or LOW).
            4. Summarize the overall security and quality of the code, explaining how the knowledge graph information assisted in your assessment.
            5. If you feel confident in doing so, provide a security score for the code on a scale of 0-100.
            
            When referencing information from the knowledge graph in your analysis, clearly indicate this by mentioning "According to the knowledge graph" or similar phrasing.
            
            Your final output should be strictly in the JSON format described above, with no additional text or explanations outside of this structure. Ensure that all fields in the JSON structure are properly filled out based on your analysis.
            """, language, kgDataJson, generatedCode);
    }

    /**
     * 从 LLM 的响应中解析 VulnerabilityReport。
     */
    private VulnerabilityReport parseVulnerabilityReportFromLlmResponse(String llmResponse, String codeId) {
        try {
            // 尝试提取被 ```json ... ``` 包裹的内容
            Pattern jsonBlockPattern = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```", Pattern.MULTILINE);
            Matcher matcher = jsonBlockPattern.matcher(llmResponse);
            String jsonToParse = llmResponse;
            if (matcher.find()) {
                jsonToParse = matcher.group(1).trim();
            }

            VulnerabilityReport report = objectMapper.readValue(jsonToParse, VulnerabilityReport.class);
            if (report.getId() == null) {
                report.setId(UUID.randomUUID().toString()); // 确保报告有ID
            }
            report.setCodeId(codeId);
            if (report.getScanTime() == null) {
                report.setScanTime(LocalDateTime.now());
            }
            return report;
        } catch (JsonProcessingException e) {
            log.error("代码 {}：从LLM响应解析 VulnerabilityReport JSON 失败: {} - 响应内容: {}", codeId, e.getMessage(), llmResponse.substring(0, Math.min(llmResponse.length(), 500)));
            // 返回一个空的或包含错误信息的报告
            VulnerabilityReport errorReport = new VulnerabilityReport();
            errorReport.setId(UUID.randomUUID().toString());
            errorReport.setCodeId(codeId);
            errorReport.setScanTime(LocalDateTime.now());
            errorReport.setSummary("无法从AI响应中解析详细的漏洞报告。原始响应可能格式不正确。");
            errorReport.setVulnerabilities(Collections.emptyList());
            return errorReport;
        }
    }

    /**
     * 获取知识图谱的 Schema 描述
     */
    private String getKnowledgeGraphSchemaDescription() {
        return """
            Node Types:
            - Vulnerability: {cweId: string (unique identifier, e.g., CWE-ID), name: string, severity: string (CRITICAL, HIGH, MEDIUM, LOW), description: string}
            - CodePattern: {patternId: string (unique identifier), language: string, codeSnippet: string, description: string, line: integer (optional)}
            - ModelDetection: {detectionId: string (unique identifier), modelVersion: string, timestamp: datetime}
            - GeneratedCode: {id: string (unique identifier), language: string, prompt: string, aiModel: string}
            
            Relationship Types:
            - (CodePattern)-[:MANIFESTS_IN]->(Vulnerability) // Code pattern manifests a certain vulnerability
            - (ModelDetection)-[:IDENTIFIES {confidence: float}]->(Vulnerability) // AI model detection identifies a vulnerability
            - (GeneratedCode)-[:CONTAINS_PATTERN]->(CodePattern) // Generated code contains a certain code pattern (if applicable)
            - (GeneratedCode)-[:HAS_VULNERABILITY]->(Vulnerability) // Generated code has a certain vulnerability (if applicable)
            
            Main Query Objectives:
            - Find related CodePatterns and Vulnerabilities based on code characteristics (language, library, functionality).
            - Find common CodePatterns for a specific Vulnerability (e.g., by cweId).
            - Find Vulnerabilities associated with a certain CodePattern.
            """;
    }
}
