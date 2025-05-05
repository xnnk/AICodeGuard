package com.ai.aicodeguard.infrastructure.graph.task;

import com.ai.aicodeguard.infrastructure.graph.exception.GraphServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * @ClassName: KnowledgeGraphMaintenanceTask
 * @Description: 知识图谱维护任务类
 * @Author: LZX
 * @Date: 2025/4/28 16:05
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGraphMaintenanceTask {

    private final Driver neo4jDriver;

    /**
     * 每周执行一次知识图谱优化
     */
    @Scheduled(cron = "0 0 2 ? * SUN") // 每周日凌晨2点执行
    public void optimizeGraph() {
        log.info("开始执行知识图谱维护任务");

        try (Session session = neo4jDriver.session()) {
            // 1. 合并相似漏洞节点
            String mergeSimilarVulnerabilities =
                    "MATCH (v1:Vulnerability), (v2:Vulnerability) " +
                            "WHERE v1.name = v2.name AND id(v1) <> id(v2) " +
                            "WITH v1, v2, count(*) as cnt " +
                            "LIMIT 100 " +
                            "MATCH (cp:CodePattern)-[r:MANIFESTS_IN]->(v2) " +
                            "CREATE (cp)-[:MANIFESTS_IN]->(v1) " +
                            "WITH v1, v2 " +
                            "MATCH (md:ModelDetection)-[r2:IDENTIFIES]->(v2) " +
                            "CREATE (md)-[:IDENTIFIES {confidence: r2.confidence}]->(v1) " +
                            "RETURN count(*)";

            session.run(mergeSimilarVulnerabilities);

            // 2. 清理孤立节点
            String cleanupIsolatedNodes =
                    "MATCH (n) " +
                            "WHERE NOT (n)--() " +
                            "WITH n LIMIT 1000 " +
                            "DELETE n " +
                            "RETURN count(*)";

            session.run(cleanupIsolatedNodes);

            log.info("知识图谱维护任务完成");
        } catch (Neo4jException e) {
            log.error("知识图谱维护失败", e);
            throw new GraphServiceException("知识图谱维护失败: " + e.getMessage());
        }
    }
}
