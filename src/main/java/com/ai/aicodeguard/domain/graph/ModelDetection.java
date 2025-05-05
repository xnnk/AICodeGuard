package com.ai.aicodeguard.domain.graph;

import jakarta.persistence.PrePersist;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @ClassName: ModelDetection
 * @Description: 模型检测节点类
 * @Author: LZX
 * @Date: 2025/4/28 15:09
 */
@Node("ModelDetection")
public class ModelDetection {
    @Id
    @GeneratedValue
    private Long id;

    @Property(name = "detectionId")
    private String detectionId;

    @Property(name = "modelVersion")
    private String modelVersion;

    @Property(name = "timestamp")
    @CreatedDate
    private LocalDateTime timestamp;

    @Relationship(type = "IDENTIFIES")
    private IdentifiesRelationship identifiesVulnerability;

    @PrePersist
    public void generateDetectionId() {
        if (this.detectionId == null) {
            this.detectionId = "MD_" + UUID.randomUUID().toString();
        }
    }
}
