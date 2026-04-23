package com.teamresource.user.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String email,
        String displayName,
        String timezone,
        String roleSummary,
        String accountStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
