package com.teamresource.event.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clients")
public record ClientProperties(
        String workflowServiceBaseUrl,
        String workflowServiceApiKey,
        String internalApiHeaderName
) {
}
