package com.teamresource.notification.domain.model;

import java.util.UUID;

public record NotificationSearchCriteria(
        UUID userId,
        NotificationStatus status,
        NotificationChannel channel,
        NotificationType type,
        Boolean unreadOnly
) {
}
