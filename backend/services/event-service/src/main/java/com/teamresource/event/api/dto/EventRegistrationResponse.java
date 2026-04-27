package com.teamresource.event.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventRegistrationResponse(
        UUID registrationId,
        UUID eventId,
        UUID userId,
        String status,
        Integer waitlistPosition,
        OffsetDateTime registeredAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime updatedAt
) {
}
