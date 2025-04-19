package com.ai.aicodeguard.domain.codegen.document;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * @ClassName: GeneratedCodeDocument
 * @Description: MongoDB中存储的生成代码文档
 * @Author: LZX
 * @Date: 2025/4/18 13:41
 */
@Data
@Document(collection = "generated_codes")
public class GeneratedCodeDocument {

    /**
     * 代码ID（与MySQL的generated_code.id对应）
     */
    @Id
    private String id;

    /**
     * 生成的代码内容
     */
    private String content;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 用户输入的自然语言需求
     */
    private String prompt;

    /**
     * 使用的AI模型
     */
    private String aiModel;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 生成时间
     */
    private LocalDateTime createdAt;

    /**
     * 检测状态（冗余字段，便于查询）
     */
    private String scanStatus;
}
