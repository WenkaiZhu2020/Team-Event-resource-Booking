package com.teamresource.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integrations.user-service")
public record UserServiceIntegrationProperties(
        boolean enabled,
        String baseUrl,
        String apiKey
) {
}
