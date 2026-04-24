package com.teamresource.resource.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpsertResourceRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 2000) String description,
        @NotBlank String type,
        @NotBlank @Size(max = 180) String location,
        @Min(1) @Max(100000) Integer capacity,
        String approvalMode,
        Boolean allowWaitlist,
        @Min(15) @Max(1440) Integer maxBookingDurationMinutes,
        @Min(1) @Max(365) Integer advanceBookingWindowDays,
        @Valid @NotEmpty List<AvailabilityRuleRequest> availabilityRules
) {
}
