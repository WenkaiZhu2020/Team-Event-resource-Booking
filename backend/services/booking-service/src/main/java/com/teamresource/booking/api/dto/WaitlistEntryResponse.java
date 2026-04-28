package com.teamresource.booking.api.dto;

import com.teamresource.booking.domain.model.WaitlistStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WaitlistEntryResponse(
        UUID id,
        Long position,
        WaitlistStatus status,
        OffsetDateTime promotedAt
) {
}
