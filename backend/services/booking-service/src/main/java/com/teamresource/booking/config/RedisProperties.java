package com.teamresource.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
public record RedisProperties(
        boolean enabled,
        String address,
        String password,
        int database
) {
}
