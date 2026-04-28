package com.teamresource.booking.api.dto;

import com.teamresource.booking.domain.model.BookingStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingListQuery(
        UUID resourceId,
        BookingStatus status,
        OffsetDateTime from,
        OffsetDateTime to,
        int page,
        int size
) {
}
