package com.ai.aicodeguard.infrastructure.persistence;

import com.ai.aicodeguard.domain.codegen.GeneratedCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @InterfaceName: GeneratedCodeRepository
 * @Description: 代码生成记录仓库
 * @Author: LZX
 * @Date: 2025/4/18 13:53
 */
@Repository
public interface GeneratedCodeRepository extends JpaRepository<GeneratedCode, String> {

    /**
     * 根据用户ID查询代码生成记录
     */
    List<GeneratedCode> findByUserIdOrderByCreatedAtDesc(Integer userId);

    /**
     * 查询用户最近的代码生成记录
     */
    @Query("""
            SELECT g FROM GeneratedCode g
            WHERE g.userId = :userId
            ORDER BY g.createdAt DESC
            """)
    List<GeneratedCode> findRecentByUser(@Param("userId") Integer userId, org.springframework.data.domain.Pageable pageable);

    /**
     * 根据检测状态查询
     */
    List<GeneratedCode> findByScanStatus(GeneratedCode.ScanStatus status);
}
