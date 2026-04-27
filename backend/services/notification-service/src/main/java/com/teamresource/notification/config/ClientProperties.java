package com.teamresource.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clients")
public record ClientProperties(
        String eventServiceBaseUrl,
        String eventServiceApiKey,
        String internalApiHeaderName
) {
}
