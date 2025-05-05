package com.ai.aicodeguard.domain.graph;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

/**
 * @ClassName: IdentifiesRelationship
 * @Description: 识别漏洞的关系类
 * @Author: LZX
 * @Date: 2025/4/28 15:10
 */
@Data
@RelationshipProperties
public class IdentifiesRelationship {
    @Id
    @GeneratedValue
    private Long id;  // 添加生成的ID字段

    @TargetNode
    private Vulnerability vulnerability;

    @Property(name = "confidence")
    private Double confidence;

    public IdentifiesRelationship(Vulnerability vulnerability) {
        this.vulnerability = vulnerability;
        this.confidence = 1.0; // 默认信心值
    }

    public IdentifiesRelationship(Vulnerability vulnerability, double confidence) {
        this.vulnerability = vulnerability;
        this.confidence = confidence;
    }
}
