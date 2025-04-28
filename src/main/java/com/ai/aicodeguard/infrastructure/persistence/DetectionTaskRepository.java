package com.ai.aicodeguard.infrastructure.persistence;

import com.ai.aicodeguard.domain.codegen.DetectionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @InterfaceName: DetectionTaskRepository
 * @Description: 漏洞检测任务记录仓库
 * @Author: LZX
 * @Date: 2025/4/18 13:56
 */
@Repository
public interface DetectionTaskRepository extends JpaRepository<DetectionTask, String> {

    /**
     * 根据代码ID查询任务
     */
    Optional<DetectionTask> findByCodeId(String codeId);

    /**
     * 根据状态查询任务
     */
    List<DetectionTask> findByStatus(DetectionTask.TaskStatus status);

    /**
     * 根据代码ID和状态查询任务
     */
    Optional<DetectionTask> findByCodeIdAndStatus(String codeId, DetectionTask.TaskStatus status);

    /**
     * 根据代码ID查询最新的任务
     */
    Optional<DetectionTask> findFirstByCodeIdOrderByStartTimeDesc(String codeId);
}
