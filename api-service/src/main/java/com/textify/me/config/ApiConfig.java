package com.textify.me.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class ApiConfig {

    /**
     * Defines a custom ThreadPoolTaskExecutor bean for Pub/Sub APIFuture callbacks.
     * This ensures callbacks run on a managed thread pool, not blocking the main threads.
     */
    @Bean(name = "pubSubCallbackExecutor")
    public Executor pubSubCallbackExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Number of threads to keep in the pool, even if idle
        executor.setMaxPoolSize(10); // Maximum number of threads in the pool
        executor.setQueueCapacity(25); // Capacity for the ThreadPoolTaskExecutor's blocking queue
        executor.setThreadNamePrefix("PubSubCallback-"); // Prefix for thread names (helpful for logging)
        executor.initialize(); // Initialize the thread pool
        return executor;
    }
}