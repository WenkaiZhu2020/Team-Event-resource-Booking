package com.teamresource.analytics.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ResourcePopularityResponse(
        UUID resourceId,
        String resourceName,
        String resourceType,
        long totalBookings,
        long approvedBookings,
        long pendingBookings,
        long waitlistedBookings,
        long cancelledBookings,
        long totalReservedMinutes,
        BigDecimal popularityScore,
        OffsetDateTime lastRefreshedAt
) {
}
