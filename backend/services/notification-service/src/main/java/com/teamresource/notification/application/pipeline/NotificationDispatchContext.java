package com.teamresource.notification.application.pipeline;

import com.teamresource.notification.infrastructure.persistence.entity.NotificationEntity;
import java.util.Map;

public record NotificationDispatchContext(
        NotificationEntity notification,
        Map<String, Object> templateData
) {
}
