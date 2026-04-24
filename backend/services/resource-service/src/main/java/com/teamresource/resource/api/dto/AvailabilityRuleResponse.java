package com.teamresource.resource.api.dto;

import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityRuleResponse(
        UUID availabilityRuleId,
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean available
) {
}
