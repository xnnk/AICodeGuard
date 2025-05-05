package com.ai.aicodeguard.domain.graph;

import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

import java.util.UUID;

/**
 * @ClassName: CodePattern
 * @Description: 代码模式节点类
 * @Author: LZX
 * @Date: 2025/4/28 15:07
 */
@Data
@AllArgsConstructor
@Node("CodePattern")
public class CodePattern {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 代码模式ID
     */
    @Property(name = "patternId")
    private String patternId;

    /**
     * 代码模式名称
     */
    @Property(name = "language")
    private String language;

    /**
     * 代码模式描述
     */
    @Property(name = "codeSnippet")
    private String codeSnippet;

    /**
     * 代码模式类型(无属性关系类)
     */
    @Relationship(type = "MANIFESTS_IN", direction = Relationship.Direction.OUTGOING)
    private Vulnerability vulnerability;

    /**
     * 代码模式创建时间
     */
    @PrePersist
    public void generatePatternId() {
        if (this.patternId == null) {
            this.patternId = "CP_" + UUID.randomUUID().toString();
        }
    }
}
