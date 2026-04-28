package com.teamresource.notification.infrastructure.config;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.channels")
@Profile("source-architecture")
public record ChannelProperties(
        Email email
) {
    public record Email(
            boolean enabled,
            String from
    ) {
    }
}
