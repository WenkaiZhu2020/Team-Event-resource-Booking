package com.teamresource.resource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.internal-api")
public record InternalApiProperties(
        String keyHeaderName,
        String keyValue
) {
}
