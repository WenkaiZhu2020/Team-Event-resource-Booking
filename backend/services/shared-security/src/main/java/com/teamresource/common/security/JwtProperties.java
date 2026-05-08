package com.teamresource.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String issuer,
        String secretBase64,
        Long accessTokenMinutes
) {
    public long requiredAccessTokenMinutes() {
        if (accessTokenMinutes == null) {
            throw new IllegalStateException("app.security.jwt.access-token-minutes is required for token generation");
        }
        return accessTokenMinutes;
    }
}
