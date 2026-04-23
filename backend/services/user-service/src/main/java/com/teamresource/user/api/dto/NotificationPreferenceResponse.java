package com.teamresource.user.api.dto;

public record NotificationPreferenceResponse(
        boolean inAppEnabled,
        boolean emailEnabled,
        int reminderMinutesBefore
) {
}
