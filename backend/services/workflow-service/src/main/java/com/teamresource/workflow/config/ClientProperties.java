package com.teamresource.workflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clients")
public record ClientProperties(
        String bookingServiceBaseUrl,
        String bookingServiceApiKey,
        String internalApiHeaderName
) {
}
