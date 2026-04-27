package com.teamresource.notification.service.template;

import com.teamresource.notification.domain.NotificationType;

public record RenderedNotification(
        NotificationType type,
        String subject,
        String body
) {
}
