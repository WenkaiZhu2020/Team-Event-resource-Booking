package com.teamresource.notification.infrastructure.config;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
@Profile("source-architecture")
public record MessagingProperties(
        String inboundExchange,
        String inboundRoutingKey,
        Consumers consumers,
        Retry retry,
        Reminder reminder
) {
    public record Consumers(
            String bookingEventsQueue,
            String bookingEventsDlq
    ) {
    }

    public record Retry(
            int maxAttempts,
            int backoffSeconds
    ) {
    }

    public record Reminder(
            int pollingSeconds
    ) {
    }
}
