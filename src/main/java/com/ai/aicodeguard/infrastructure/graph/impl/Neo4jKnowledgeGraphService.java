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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
        Map<String, String> uniqueStatements = new HashMap<>();

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
     * 例如：从"MERGE (v:Vulnerability {..."提取"Vulnerability"
     */
    private String extractNodeType(String statement) {
        int labelStart = statement.indexOf(':');
        if (labelStart == -1) return null;

        int propertyStart = statement.indexOf('{', labelStart);
        if (propertyStart == -1) return null;

        return statement.substring(labelStart + 1, propertyStart).trim();
    }

    /**
     * 提取MERGE语句中的主键值
     * 例如：从"MERGE (v:Vulnerability {cweId: 'CWE-89'..."提取"CWE-89"
     */
    private String extractNodeKey(String statement) {
        // 简化实现，针对特定格式提取
        // 实际应用中可能需要更复杂的解析逻辑
        if (statement.contains("cweId:")) {
            int start = statement.indexOf("cweId:") + 7;
            int end = statement.indexOf("'", start + 1);
            if (start > 7 && end > start) {
                return statement.substring(start, end);
            }
        } else if (statement.contains("patternId:")) {
            int start = statement.indexOf("patternId:") + 11;
            int end = statement.indexOf("'", start + 1);
            if (start > 11 && end > start) {
                return statement.substring(start, end);
            }
        }

        return null;
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
        // 使用claude3.5sonnet来生成cypher
        AIClientService aiClient = aiClientFactory.getClient("claude");

        // 构建提示词
        String prompt = buildGraphPrompt(codeContent, language, vulnerability);

        // 调用AI生成Cypher
        String cypherQuery = aiClient.generateCode(prompt, "cypher");

        // 清理AI输出，提取实际的Cypher语句
        return extractCypherQuery(cypherQuery);
    }

    // TODO: 目前有点BUG，这里会重复生成相同名称的节点，我需要引导大模型使用节点名称来创建节点
    /**
     * 构建用于生成Cypher的提示词
     */
    private String buildGraphPrompt(String codeContent, String language, VulnerabilityReport.Vulnerability vulnerability) {
        return String.format("""
            你是一个Neo4j知识图谱专家，请根据以下规则生成Cypher语句：
          
            ### 强制格式规范
            1. **节点全属性声明**：每个MERGE语句必须完整声明节点所有定义属性
            2. **禁止属性简写**：禁止使用SET子句补充属性，所有属性必须在MERGE时完整声明
            3. **节点独立定义**：禁止跨语句引用节点变量（如v1, cp1等）
            4. **属性值规范**：字符串用单引号、时间用datetime()
            5. **关系精确锚定**：关系必须通过属性值直接锚定，禁止使用变量引用
            6. **禁止使用RETURN**：禁止使用RETURN语句，所有Cypher必须以分号结尾
            7. **无修复建议**：如果没有修复建议，返回空字符串
            8. Vulnerability节点的severity属性必须是CRITICAL/HIGH/MEDIUM/LOW之一

            ### 知识图谱模式定义
            **节点类型**（必须包含全部属性）：
            - Vulnerability {cweId: string, name: string, severity: string}
            - CodePattern {patternId: string, language: string, codeSnippet: string, line: integer}
            - ModelDetection {detectionId: string, modelVersion: string, timestamp: datetime}
  
            **关系类型**（带属性必须声明）：
            - (CodePattern{patternId: string})-[MANIFESTS_IN]->(Vulnerability{cweId: string})
            - (ModelDetection{detectionId: string})-[IDENTIFIES {confidence: float}]->(Vulnerability{cweId: string})

            ### 输入数据
            代码语言：%s
            代码内容：%s
            漏洞类型（含CWE）：%s
            严重等级：%s
            漏洞位置：%d
            修复建议：%s
            ID后缀：%s

            ### 生成规则
            1. 每个节点单独MERGE，使用完整属性匹配
            2. 关系通过节点全名和节点唯一属性值直接建立，示例：
               MERGE (c:CodePattern {patternId:'cp_ID后缀', language:'Java'});
               MERGE (v:Vulnerability {cweId:'CWE-79', name:'XSS', severity:'CRITICAL'});
               MERGE (c:CodePattern {patternId:'cp_ID后缀', language:'Java'})-[r:MANIFESTS_IN]->(v:Vulnerability {cweId:'CWE-79', name:'XSS', severity:'CRITICAL'});
            3. 不需要生成唯一性ID，关系必须通过属性值直接锚定，确保禁止使用变量引用
            4. 时间戳统一用datetime().epochMillis形式
            5. 置信度计算：请根据输入数据给出置信度，范围0-1
            6. CodePattern和ModelDetection节点的patternId和detectionId必须包含ID后缀
            7. ModelDetection的modelVersion必须是claude-3-7-sonnet

            ### 违规控制
            1. **简写拦截**：检测到SET子句立即终止生成
            2. **属性缺失检测**：节点缺少任意定义属性则重新生成
            3. **变量引用阻断**：发现节点变量引用（如使用单个v, cp）直接报错

            请输出符合规范的Cypher语句，严格确保节点全属性声明。
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
        // 处理可能的代码块
        if (aiOutput.contains("```cypher")) {
            int start = aiOutput.indexOf("```cypher") + 9;
            int end = aiOutput.indexOf("```", start);
            if (end > start) {
                return aiOutput.substring(start, end).trim();
            }
        }

        if (aiOutput.contains("```")) {
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
}
