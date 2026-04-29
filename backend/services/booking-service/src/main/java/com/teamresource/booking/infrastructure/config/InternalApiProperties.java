package com.teamresource.booking.infrastructure.config;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.internal-api")
@Profile("stage2-layered-inactive")
public record InternalApiProperties(String keyHeaderName, String keyValue) {
}
