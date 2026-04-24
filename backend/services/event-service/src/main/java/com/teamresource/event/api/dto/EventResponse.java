package com.teamresource.event.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(
        UUID eventId,
        UUID organizerId,
        String title,
        String description,
        String category,
        String location,
        int capacity,
        OffsetDateTime registrationOpenAt,
        OffsetDateTime registrationCloseAt,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
