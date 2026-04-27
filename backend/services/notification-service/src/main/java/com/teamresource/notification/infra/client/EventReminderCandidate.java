package com.teamresource.notification.infra.client;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventReminderCandidate(
        UUID registrationId,
        UUID eventId,
        UUID userId,
        String eventTitle,
        String location,
        OffsetDateTime startAt
) {
}
