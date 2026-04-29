package com.teamresource.booking.infrastructure.config;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
@Profile("stage2-layered-inactive")
public record MessagingProperties(
        String exchange,
        Outbox outbox,
        Consumers consumers
) {
    public record Outbox(
            int batchSize,
            int retryDelaySeconds
    ) {
    }

    public record Consumers(
            String eventResourceQueue,
            String eventResourceDlq
    ) {
    }
}
