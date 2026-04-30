package com.teamresource.analytics.api.dto;

import java.util.UUID;

public record EventRegistrationMetricResponse(
        UUID eventId,
        long activeBookings,
        long waitlistedBookings,
        long cancelledBookings
) {
}
