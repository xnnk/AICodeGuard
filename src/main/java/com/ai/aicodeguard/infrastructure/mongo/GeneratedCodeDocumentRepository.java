package com.ai.aicodeguard.infrastructure.mongo;

import com.ai.aicodeguard.domain.codegen.document.GeneratedCodeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @ClassName: GeneratedCodeDocumentRepository
 * @Description: MongoDB中生成代码文档仓库
 * @Author: LZX
 * @Date: 2025/4/18 13:59
 */
@Repository
public interface GeneratedCodeDocumentRepository extends MongoRepository<GeneratedCodeDocument, String> {

    /**
     * 根据用户ID查询代码文档
     */
    List<GeneratedCodeDocument> findByUserIdOrderByCreatedAtDesc(Integer userId);

    /**
     * 根据检测状态查询代码文档
     */
    List<GeneratedCodeDocument> findByScanStatus(String scanStatus);
}
