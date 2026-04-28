package com.teamresource.notification.api.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record BatchReadRequest(
        @NotEmpty(message = "notificationIds must not be empty")
        List<UUID> notificationIds
) {
}
