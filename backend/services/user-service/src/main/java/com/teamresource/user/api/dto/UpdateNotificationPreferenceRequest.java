package com.teamresource.user.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateNotificationPreferenceRequest(
        boolean inAppEnabled,
        boolean emailEnabled,
        @Min(0) @Max(10080) int reminderMinutesBefore
) {
}
