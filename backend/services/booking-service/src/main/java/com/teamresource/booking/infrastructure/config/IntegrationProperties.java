package com.teamresource.booking.infrastructure.config;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integration")
@Profile("source-architecture")
public record IntegrationProperties(
        String resourceServiceBaseUrl,
        String resourceServiceApiKey
) {
}
