package com.teamresource.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.reminders")
public record ReminderProperties(
        boolean enabled,
        long fixedDelayMs,
        long initialDelayMs,
        int lookaheadHours
) {
}
