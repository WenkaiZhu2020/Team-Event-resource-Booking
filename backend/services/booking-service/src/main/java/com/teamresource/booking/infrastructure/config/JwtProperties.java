package com.teamresource.booking.infrastructure.config;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
@Profile("source-architecture")
public record JwtProperties(
        String issuer,
        String secretBase64
) {
}
