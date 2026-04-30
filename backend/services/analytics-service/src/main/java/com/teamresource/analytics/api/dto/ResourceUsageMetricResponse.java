package com.teamresource.analytics.api.dto;

import java.util.UUID;

public record ResourceUsageMetricResponse(
        UUID resourceId,
        long totalBookings,
        long approvedBookings,
        long pendingBookings,
        long cancelledBookings,
        long bookedMinutes
) {
}
