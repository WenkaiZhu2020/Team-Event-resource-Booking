package com.teamresource.notification.api.dto;

import java.util.UUID;

public record NotificationPreferenceResponse(
        UUID userId,
        boolean inAppEnabled,
        boolean emailEnabled,
        int reminderLeadMinutes
) {
}
