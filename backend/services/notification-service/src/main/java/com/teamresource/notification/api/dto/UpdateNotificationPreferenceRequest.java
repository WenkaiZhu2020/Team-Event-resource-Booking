package com.teamresource.notification.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateNotificationPreferenceRequest(
        boolean inAppEnabled,
        boolean emailEnabled,
        @Min(5) @Max(1440) int reminderLeadMinutes
) {
}
