package com.teamresource.event.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventReminderCandidateResponse(
        UUID registrationId,
        UUID eventId,
        UUID userId,
        String eventTitle,
        String location,
        OffsetDateTime startAt
) {
}
