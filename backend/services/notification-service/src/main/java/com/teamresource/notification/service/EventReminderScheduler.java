package com.teamresource.notification.service;

import com.teamresource.notification.config.ReminderProperties;
import com.teamresource.notification.infra.client.EventServiceClient;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventReminderScheduler.class);

    private final ReminderProperties properties;
    private final EventServiceClient eventServiceClient;
    private final NotificationService notificationService;

    public EventReminderScheduler(
            ReminderProperties properties,
            EventServiceClient eventServiceClient,
            NotificationService notificationService
    ) {
        this.properties = properties;
        this.eventServiceClient = eventServiceClient;
        this.notificationService = notificationService;
    }

    @Scheduled(
            fixedDelayString = "${app.reminders.fixed-delay-ms:900000}",
            initialDelayString = "${app.reminders.initial-delay-ms:30000}"
    )
    public void scheduleEventReminders() {
        if (!properties.enabled()) {
            return;
        }

        OffsetDateTime windowStart = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime windowEnd = windowStart.plusHours(properties.lookaheadHours());
        try {
            eventServiceClient.fetchDueReminders(windowStart, windowEnd)
                    .forEach(notificationService::sendEventReminder);
        } catch (Exception ex) {
            log.warn("Event reminder dispatch failed: {}", ex.getMessage());
        }
    }
}
