package com.teamresource.notification.service.template;

import com.teamresource.notification.domain.NotificationType;
import com.teamresource.notification.infra.client.EventReminderCandidate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class EventReminderTemplateRenderer {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'");

    public RenderedNotification render(EventReminderCandidate reminder) {
        String startAt = reminder.startAt().withOffsetSameInstant(java.time.ZoneOffset.UTC).format(FORMATTER);
        return new RenderedNotification(
                NotificationType.EVENT_REMINDER,
                "Upcoming event: " + reminder.eventTitle(),
                "Reminder: " + reminder.eventTitle()
                        + " starts at " + startAt
                        + " in " + reminder.location() + "."
        );
    }
}
