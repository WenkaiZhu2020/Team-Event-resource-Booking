package com.teamresource.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.analytics")
public record AnalyticsProperties(
        String queueName,
        String deadLetterQueueName,
        long popularityRefreshMs,
        int dashboardThreadPoolSize
) {
}
