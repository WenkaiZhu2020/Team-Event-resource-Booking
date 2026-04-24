package com.teamresource.resource.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ResourceResponse(
        UUID resourceId,
        UUID managerId,
        String name,
        String description,
        String type,
        String location,
        Integer capacity,
        String status,
        String approvalMode,
        boolean requiresApproval,
        boolean allowWaitlist,
        int maxBookingDurationMinutes,
        int advanceBookingWindowDays,
        List<AvailabilityRuleResponse> availabilityRules,
        List<MaintenanceSlotResponse> maintenanceSlots,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
