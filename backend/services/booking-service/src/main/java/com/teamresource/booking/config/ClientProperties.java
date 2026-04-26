package com.teamresource.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clients")
public record ClientProperties(
        String resourceServiceBaseUrl,
        String eventServiceBaseUrl
) {
}
