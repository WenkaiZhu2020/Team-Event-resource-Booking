package com.teamresource.booking.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingSearchCriteria(
        UUID userId,
        UUID resourceId,
        BookingStatus status,
        OffsetDateTime from,
        OffsetDateTime to
) {
}
