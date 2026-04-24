package com.teamresource.resource.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MaintenanceSlotResponse(
        UUID maintenanceSlotId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String reason,
        String status
) {
}
