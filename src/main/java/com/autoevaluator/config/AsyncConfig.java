package com.autoevaluator.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "externalTaskExecutor")
    public Executor externalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core thread pool for external async tasks (OCR, evaluation, etc.)
        executor.setCorePoolSize(2);        // Minimum active threads
        executor.setMaxPoolSize(4);         // Max threads for high load
        executor.setQueueCapacity(50);      // Task queue before new threads spawn

        executor.setThreadNamePrefix("AsyncExternal-");
        executor.initialize();

        // ⚠️ When deploying on larger plans (e.g., 2GB/8CPU), increase pool/queue size
        // to better handle high concurrency from multiple external service calls.

        return executor;
    }
}

