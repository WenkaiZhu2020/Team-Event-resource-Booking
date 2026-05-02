package com.teamresource.booking.service;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingCompensatedEvent(
        UUID bookingId,
        UUID resourceId,
        String previousStatus,
        String compensatedStatus,
        String compensationSource,
        String reason,
        String correlationId,
        OffsetDateTime compensatedAt
) {
}
