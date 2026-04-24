package com.teamresource.resource.service.policy;

import com.teamresource.resource.domain.ApprovalMode;

public record ResourcePolicyDefaults(
        ApprovalMode approvalMode,
        boolean allowWaitlist,
        int maxBookingDurationMinutes,
        int advanceBookingWindowDays
) {
}
