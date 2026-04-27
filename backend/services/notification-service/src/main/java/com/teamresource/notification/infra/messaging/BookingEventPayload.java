package com.teamresource.notification.infra.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingEventPayload(
        UUID bookingId,
        UUID userId,
        UUID linkedEventId,
        UUID resourceId,
        String resourceName,
        UUID resourceManagerId,
        String resourceType,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String purpose,
        String status,
        String approvalMode,
        Integer waitlistPosition,
        OffsetDateTime approvalRequestedAt,
        OffsetDateTime decidedAt,
        String decisionNote,
        OffsetDateTime cancelledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
