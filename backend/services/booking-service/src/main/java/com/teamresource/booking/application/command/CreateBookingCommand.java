package com.teamresource.booking.application.command;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateBookingCommand(
        String idempotencyKey,
        UUID userId,
        UUID eventId,
        UUID resourceId,
        OffsetDateTime startAt,
        OffsetDateTime endAt
) {
}
