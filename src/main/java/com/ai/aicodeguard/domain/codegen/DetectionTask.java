package com.ai.aicodeguard.domain.codegen;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @ClassName: DetectionTask
 * @Description: 漏洞检测任务记录
 * @Author: LZX
 * @Date: 2025/4/18 13:40
 */
@Data
@Entity
@Table(name = "detection_task")
public class DetectionTask {

    /**
     * 任务ID（UUID）
     */
    @Id
    private String id;

    /**
     * 关联的代码ID
     */
    private String codeId;

    /**
     * 任务状态
     */
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.PENDING;

    /**
     * 任务开始时间
     */
    private LocalDateTime startTime;

    /**
     * 任务结束时间
     */
    private LocalDateTime endTime;

    /**
     * 失败时的错误信息
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING, SUCCESS, FAILED
    }
}
