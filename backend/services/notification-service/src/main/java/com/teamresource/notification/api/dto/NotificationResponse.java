package com.teamresource.notification.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID notificationId,
        UUID userId,
        UUID sourceEventId,
        String sourceEventType,
        String notificationType,
        String channel,
        String subject,
        String body,
        String status,
        OffsetDateTime readAt,
        OffsetDateTime sentAt,
        String failureReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
