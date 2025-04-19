package com.ai.aicodeguard.domain.codegen;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @ClassName: GeneratedCode
 * @Description: 代码生成记录
 * @Author: LZX
 * @Date: 2025/4/18 13:38
 */
@Data
@Entity
@Table(name = "generated_code")
public class GeneratedCode {

    /**
     * 代码ID（UUID）
     */
    @Id
    private String id;

    /**
     * 生成代码的用户ID
     */
    private Integer userId;

    /**
     * 用户输入的自然语言需求
     */
    @Column(columnDefinition = "TEXT")
    private String prompt;

    /**
     * 编程语言（如Java、Python）
     */
    private String language;

    /**
     * 生成时间
     */
    private LocalDateTime createdAt;

    /**
     * 调用的AI模型（如DeepSeek）
     */
    private String aiModel;

    /**
     * 检测状态
     */
    @Enumerated(EnumType.STRING)
    private ScanStatus scanStatus = ScanStatus.PENDING;

    /**
     * 检测完成时间
     */
    private LocalDateTime scanTime;

    /**
     * 扫描状态枚举
     */
    public enum ScanStatus {
        PENDING, SUCCESS, FAILED
    }
}
