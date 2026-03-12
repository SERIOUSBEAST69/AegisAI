package com.trustai.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String AUDIT_LOG_QUEUE = "audit.log.queue";
    public static final String SCAN_TASK_QUEUE = "scan.task.queue";

    @Bean
    public Queue auditLogQueue() {
        return new Queue(AUDIT_LOG_QUEUE, true);
    }

    @Bean
    public Queue scanTaskQueue() {
        return new Queue(SCAN_TASK_QUEUE, true);
    }
}
