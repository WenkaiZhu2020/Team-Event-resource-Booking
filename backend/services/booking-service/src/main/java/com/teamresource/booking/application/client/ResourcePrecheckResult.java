package com.teamresource.booking.application.client;

public record ResourcePrecheckResult(
        boolean bookingAllowed,
        String reasonCode,
        boolean requiresApproval,
        boolean allowWaitlist,
        Integer maxBookingDurationMinutes,
        Integer bufferBeforeMinutes,
        Integer bufferAfterMinutes
) {
}
