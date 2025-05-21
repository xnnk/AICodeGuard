package com.ai.aicodeguard.infrastructure.graph.impl;

import com.ai.aicodeguard.domain.codegen.document.GeneratedCodeDocument;
import com.ai.aicodeguard.domain.codegen.document.VulnerabilityReport;
import com.ai.aicodeguard.domain.graph.CodePattern;
import com.ai.aicodeguard.domain.graph.Vulnerability;
import com.ai.aicodeguard.infrastructure.ai.AIClientFactory;
import com.ai.aicodeguard.infrastructure.ai.AIClientService;
import com.ai.aicodeguard.infrastructure.graph.KnowledgeGraphService;
import com.ai.aicodeguard.infrastructure.graph.exception.GraphServiceException;
import com.ai.aicodeguard.infrastructure.mongo.GeneratedCodeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @ClassName: Neo4jKnowledgeGraphService
 * @Description: Neo4j知识图谱服务实现类
 * @Author: LZX
 * @Date: 2025/4/28 15:59
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Neo4jKnowledgeGraphService implements KnowledgeGraphService {

    private final Driver neo4jDriver;
    private final AIClientFactory aiClientFactory;
    private final GeneratedCodeDocumentRepository codeDocumentRepository;

    @Override
    public boolean updateGraphWithVulnerabilities(String codeId, VulnerabilityReport report) {
        log.info("开始更新知识图谱，代码ID: {}", codeId);

        try {
            // 查找代码内容
            Optional<GeneratedCodeDocument> codeDocOpt = codeDocumentRepository.findById(codeId);
            if (codeDocOpt.isEmpty()) {
                log.error("未找到代码文档: {}", codeId);
                return false;
            }

            GeneratedCodeDocument codeDocument = codeDocOpt.get();
            String codeContent = codeDocument.getContent();
            String language = codeDocument.getLanguage();

            // 对每个漏洞生成并执行Cypher查询
            if (report.getVulnerabilities() != null && !report.getVulnerabilities().isEmpty()) {
                for (VulnerabilityReport.Vulnerability vulnerability : report.getVulnerabilities()) {
                    try {
                        // 使用AI生成Cypher查询
                        String cypher = generateCypherWithAI(codeContent, language, vulnerability);

                        // 去除重复的MERGE语句
                        cypher = removeDuplicateMergeStatements(cypher);

                        // 执行查询
                        if (!executeCypher(cypher)) {
                            log.warn("执行知识图谱更新Cypher查询失败，代码ID: {}, 漏洞类型: {}",
                                    codeId, vulnerability.getType());
                        }
                    } catch (Exception e) {
                        log.error("处理漏洞时出错: {}", vulnerability.getType(), e);
                        // 继续处理其他漏洞
                    }
                }
            }

            log.info("知识图谱更新完成，代码ID: {}", codeId);
            return true;
        } catch (Exception e) {
            log.error("更新知识图谱失败", e);
            throw new GraphServiceException("更新知识图谱失败: " + e.getMessage(), e);
        }
    }

    /**
     * 去除重复的MERGE语句
     */
    private String removeDuplicateMergeStatements(String cypher) {
        // 按语句分割
        String[] statements = cypher.split(";\\s*\\n+");

        /*
         * 修复了这个bug，将HashMap改为LinkedHashMap以保持插入顺序
         */
        Map<String, String> uniqueStatements = new LinkedHashMap<>();

        for (String statement : statements) {
            statement = statement.trim();
            if (statement.isEmpty()) continue;

            // 提取MERGE语句的关键部分作为去重key
            // 例如从"MERGE (v:Vulnerability {cweId: 'CWE-89'..."提取"Vulnerability CWE-89"
            if (statement.startsWith("MERGE")) {
                String nodeType = extractNodeType(statement);
                String nodeKey = extractNodeKey(statement);

                if (nodeType != null && nodeKey != null) {
                    String key = nodeType + " " + nodeKey;
                    // 只保留第一次出现的语句
                    if (!uniqueStatements.containsKey(key)) {
                        uniqueStatements.put(key, statement);
                    }
                } else {
                    // 关系语句或其他语句保留
                    uniqueStatements.put(statement, statement);
                }
            } else {
                // 非MERGE语句直接保留
                uniqueStatements.put(statement, statement);
            }
        }

        // 重新组合去重后的语句
        StringBuilder result = new StringBuilder();
        for (String statement : uniqueStatements.values()) {
            result.append(statement).append(";\n\n");
        }

        return result.toString();
    }

    /**
     * 提取MERGE语句中的节点类型
     * 从"MERGE (v:Vulnerability {..."提取"Vulnerability"
     */
    private String extractNodeType(String statement) {
        // 查找节点标签格式 如 (x:Label)
        int openParen = statement.indexOf('(');
        if (openParen == -1) return null;

        int labelStart = statement.indexOf(':', openParen);
        if (labelStart == -1) return null;

        // 找到标签结束位置（空格、大括号或右括号）
        int propertyStart = statement.indexOf('{', labelStart);
        int closeParen = statement.indexOf(')', labelStart);
        int spaceAfter = statement.indexOf(' ', labelStart);

        // 找出最近的终止符
        int end = Integer.MAX_VALUE;
        if (propertyStart > -1) end = propertyStart;
        if (closeParen > -1) end = Math.min(end, closeParen);
        if (spaceAfter > -1) end = Math.min(end, spaceAfter);

        if (end == Integer.MAX_VALUE) return null;

        return statement.substring(labelStart + 1, end).trim();
    }

    /**
     * 提取MERGE语句中的主键值
     * 从"MERGE (v:Vulnerability {cweId: 'CWE-89'..."提取"CWE-89"
     */
    private String extractNodeKey(String statement) {
        Map<String, String> keyPrefixes = Map.of(
                "Vulnerability", "cweId:",
                "CodePattern", "patternId:",
                "ModelDetection", "detectionId:"
        );

        String nodeType = extractNodeType(statement);
        if (nodeType == null || !keyPrefixes.containsKey(nodeType)) {
            return null;
        }

        String keyPrefix = keyPrefixes.get(nodeType);
        int start = statement.indexOf(keyPrefix);
        if (start == -1) return null;

        start += keyPrefix.length();
        // 跳过可能的空格
        while (start < statement.length() && Character.isWhitespace(statement.charAt(start))) {
            start++;
        }

        // 跳过单引号
        if (start < statement.length() && statement.charAt(start) == '\'') {
            start++;
        }

        int end = statement.indexOf('\'', start);
        if (end == -1) return null;

        return statement.substring(start, end);
    }

    @Override
    public String findCodePatternsForVulnerability(String language, String vulnerabilityType) {
        String query = "MATCH (cp:CodePattern)-[:MANIFESTS_IN]->(v:Vulnerability) " +
                "WHERE cp.language = $language AND v.name CONTAINS $vulnType " +
                "RETURN cp.codeSnippet AS pattern, v.name AS vulnerability " +
                "LIMIT 5";

        try (Session session = neo4jDriver.session()) {
            Map<String, Object> params = new HashMap<>();
            params.put("language", language);
            params.put("vulnType", vulnerabilityType);

            Result result = session.run(query, params);
            StringBuilder patterns = new StringBuilder();

            while (result.hasNext()) {
                Record record = result.next();
                patterns.append("漏洞类型: ").append(record.get("vulnerability").asString())
                        .append("\n代码模式:\n```").append(language).append("\n")
                        .append(record.get("pattern").asString())
                        .append("\n```\n\n");
            }

            return !patterns.isEmpty() ? patterns.toString() : "未找到相关代码模式";
        } catch (Neo4jException e) {
            log.error("查询知识图谱失败", e);
            throw new GraphServiceException("查询知识图谱失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用AI生成Cypher查询语句
     */
    private String generateCypherWithAI(String codeContent, String language, VulnerabilityReport.Vulnerability vulnerability) {
        // 使用claude3.7sonnet来生成cypher
        AIClientService aiClient = aiClientFactory.getClient("claude");

        // 构建提示词
        String prompt = buildGraphPrompt(codeContent, language, vulnerability);

        // 调用AI生成Cypher
        String cypherQuery = aiClient.generateCode(prompt, "cypher");

        // 清理AI输出，提取实际的Cypher语句
        return extractCypherQuery(cypherQuery);
    }

    /**
     * 构建用于生成Cypher的提示词
     */
    private String buildGraphPrompt(String codeContent, String language, VulnerabilityReport.Vulnerability vulnerability) {
        return String.format("""
            You are a Neo4j knowledge graph expert tasked with generating Cypher statements based on specific rules and input data. Follow these instructions carefully to create accurate and compliant Cypher statements.
            
            Knowledge Graph Schema:
            Node Types (must include all properties):
            - Vulnerability {cweId: string, name: string, severity: string}
            - CodePattern {patternId: string, language: string, codeSnippet: string, line: integer}
            - ModelDetection {detectionId: string, modelVersion: string, timestamp: datetime}
            
            Relationship Types (must declare properties if present):
            - (CodePattern{patternId: string})-[MANIFESTS_IN]->(Vulnerability{cweId: string})
            - (ModelDetection{detectionId: string})-[IDENTIFIES {confidence: float}]->(CodePattern{patternId: string})
            
            Input Data:
            You will receive the following input variables:
            <code_language>{{%s}}</code_language>
            <code_content>{{%s}}</code_content>
            <vulnerability_type>{{%s}}</vulnerability_type>
            <severity>{{%s}}</severity>
            <vulnerability_location>{{%d}}</vulnerability_location>
            <fix_suggestion>{{%s}}</fix_suggestion>
            <id_suffix>{{%s}}</id_suffix>
            
            Cypher Generation Rules:
            1. Use MERGE for each node, declaring all properties.
            2. Establish relationships using full node names and unique property values.
            3. Use datetime().epochMillis for timestamps.
            4. Calculate confidence (range 0-1) based on input data.
            5. Include the ID_SUFFIX in patternId and detectionId for CodePattern and ModelDetection nodes.
            6. Use 'claude-3-7-sonnet' as the modelVersion for ModelDetection.
            
            Instructions:
            1. Generate separate MERGE statements for each node type (Vulnerability, CodePattern, ModelDetection).
            2. Before creating relationships, use MATCH statements to find the relevant nodes by their unique IDs (cweId, patternId, detectionId).
            3. Create relationship statements using the matched nodes to form a complete Cypher statement.
            4. Ensure all required properties are included for each node and relationship.
            5. Use single quotes for string values and datetime().epochMillis for time values.
            6. Do not use variables to reference nodes across statements.
            7. Do not use SET clauses to add properties; declare all properties in the MERGE statement.
            8. Do not use RETURN statements; end all Cypher statements with a semicolon.
            9. Ensure the Vulnerability node's severity is one of CRITICAL/HIGH/MEDIUM/LOW.
            
            Error Checking and Compliance:
            - Verify that all required properties are present for each node.
            - Ensure no SET clauses are used to add properties.
            - Check that relationship creation is done by first matching relevant nodes by their IDs.
            - Confirm that relationships are established using matched nodes, forming complete Cypher statements.
            
            Output Format:
            Provide your Cypher statements within <cypher> tags. Each statement should be on a new line and end with a semicolon. Do not include any explanations or comments within these tags.
            
            Your final output should only include the <cypher>, with no additional text or explanations.
            """,
                language,
                truncateCode(codeContent), // 截断过长代码
                vulnerability.getType(),
                vulnerability.getSeverity().name(),
                vulnerability.getLine(),
                vulnerability.getSuggestion(),
                UUID.randomUUID().toString().substring(0, 8) // 为ID生成后缀
        );
    }

    /**
     * 截断过长的代码
     */
    private String truncateCode(String code) {
        // 最多保留1000个字符
        int maxLength = 1000;
        if (code.length() <= maxLength) {
            return code;
        }
        return code.substring(0, maxLength) + "...(代码已截断)";
    }

    /**
     * 从AI回复中提取Cypher语句
     */
    private String extractCypherQuery(String aiOutput) {
        // 处理 <cypher> 标签格式
        if (aiOutput.contains("<cypher>") && aiOutput.contains("</cypher>")) {
            int start = aiOutput.indexOf("<cypher>") + 8;
            int end = aiOutput.indexOf("</cypher>");
            if (end > start) {
                return aiOutput.substring(start, end).trim();
            }
        }

        // 处理可能的代码块格式
        else if (aiOutput.contains("```cypher")) {
            int start = aiOutput.indexOf("```cypher") + 9;
            int end = aiOutput.indexOf("```", start);
            if (end > start) {
                return aiOutput.substring(start, end).trim();
            }
        }

        else if (aiOutput.contains("```")) {
            int start = aiOutput.indexOf("```") + 3;
            int end = aiOutput.indexOf("```", start);
            if (end > start) {
                return aiOutput.substring(start, end).trim();
            }
        }

        // 添加最终验证和清理
        String cypher = aiOutput.trim();

        // 确保所有语句都有正确的分号结尾
        if (!cypher.endsWith(";")) {
            cypher += ";";
        }

        // 检查大括号配对
        long openBraces = cypher.chars().filter(ch -> ch == '{').count();
        long closeBraces = cypher.chars().filter(ch -> ch == '}').count();
        if (openBraces != closeBraces) {
            log.warn("Cypher语句大括号不匹配，可能导致语法错误");
        }

        return cypher;
    }

    /**
     * 执行Cypher查询
     */
    private boolean executeCypher(String cypher) {
        try (Session session = neo4jDriver.session()) {
            log.info("AI OutPut Cypher: {}", cypher);

            // 拆分Cypher语句 - 按分号和换行符分割
            String[] cypherStatements = cypher.split(";\\s*\\n+");

            for (String statement : cypherStatements) {
                // 清理语句，移除空白行
                statement = statement.trim();
                if (statement.isEmpty()) {
                    continue;
                }

                // 移除语句末尾的分号（如果存在）
                if (statement.endsWith(";")) {
                    statement = statement.substring(0, statement.length() - 1);
                }

                final String finalStatement = statement;
                // 单独执行每条语句
                session.executeWrite(tx -> {
                    tx.run(finalStatement);
                    return null;
                });
            }

            return true;
        } catch (Exception e) {
            log.error("执行Cypher失败: {}", cypher, e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> executeLlmLedReadOnlyQuery(String cypherQuery) {
        if (!StringUtils.hasText(cypherQuery)) {
            log.warn("接收到空的 Cypher 查询语句，跳过执行。");
            return java.util.Collections.emptyList();
        }
        String upperCaseQuery = cypherQuery.toUpperCase();
        if (upperCaseQuery.contains("CREATE ") || upperCaseQuery.contains("DELETE ") ||
            upperCaseQuery.contains("SET ") || upperCaseQuery.contains("REMOVE ") ||
            upperCaseQuery.contains("MERGE ")) {
            log.error("检测到非只读操作，拒绝执行 LLM 生成的 Cypher: {}", cypherQuery);
            throw new com.ai.aicodeguard.infrastructure.graph.exception.GraphServiceException("LLM 生成的 Cypher 查询包含非只读操作，执行被拒绝。");
        }
        log.debug("准备执行 LLM 生成的只读 Cypher: {}", cypherQuery);
        try (Session session = neo4jDriver.session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypherQuery);
                java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    results.add(record.asMap());
                }
                return results;
            });
        } catch (Neo4jException e) {
            log.error("执行 LLM 生成的 Cypher 查询失败: {} - 错误: {}", cypherQuery, e.getMessage(), e);
            throw new com.ai.aicodeguard.infrastructure.graph.exception.GraphServiceException("执行 LLM 生成的 Cypher 查询时发生数据库错误: " + e.getMessage(), e);
        }
    }
}

