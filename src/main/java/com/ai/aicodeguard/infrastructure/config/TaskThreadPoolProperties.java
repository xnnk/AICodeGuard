package com.ai.aicodeguard.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName: TaskThreadPoolProperties
 * @Description: 任务线程池配置
 * @Author: LZX
 * @Date: 2025/4/20 00:55
 */
@Data
@Component
@ConfigurationProperties(prefix = "task.pool")
public class TaskThreadPoolProperties {
    private int corePoolSize = 5;
    private int maxPoolSize = 30;
    private int queueCapacity = 50;
    private int keepAliveSeconds = 60;
}
