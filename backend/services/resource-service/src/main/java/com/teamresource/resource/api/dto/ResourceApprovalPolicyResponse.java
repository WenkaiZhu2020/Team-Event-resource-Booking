package com.teamresource.resource.api.dto;

import java.util.UUID;

public record ResourceApprovalPolicyResponse(
        UUID resourceId,
        UUID managerId,
        String approvalMode,
        boolean requiresApproval,
        int maxBookingDurationMinutes,
        int advanceBookingWindowDays,
        boolean allowWaitlist
) {
}
