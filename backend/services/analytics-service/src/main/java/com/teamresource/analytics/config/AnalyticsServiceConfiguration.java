package com.teamresource.analytics.config;

import java.util.concurrent.Executor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, AnalyticsProperties.class, InternalApiProperties.class})
public class AnalyticsServiceConfiguration {

    @Bean(name = "analyticsTaskExecutor")
    Executor analyticsTaskExecutor(AnalyticsProperties properties) {
        return new TaskExecutorAdapter(java.util.concurrent.Executors.newFixedThreadPool(properties.dashboardThreadPoolSize()));
    }
}
