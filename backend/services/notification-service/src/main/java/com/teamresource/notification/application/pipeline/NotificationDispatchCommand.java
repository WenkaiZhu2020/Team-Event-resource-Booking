package com.teamresource.notification.application.pipeline;

import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.domain.model.NotificationType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationDispatchCommand(
        UUID userId,
        NotificationType type,
        String templateCode,
        String source,
        String sourceEventType,
        String referenceType,
        UUID referenceId,
        String idempotencyKey,
        Map<String, Object> templateData,
        List<NotificationChannel> channels,
        OffsetDateTime scheduledAt,
        Integer maxRetries
) {
}
