package com.teamresource.booking.application.state;

import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface BookingStateHandler {

    com.teamresource.booking.domain.model.BookingStatus supports();

    String cancel(BookingEntity booking, String reason, OffsetDateTime now);

    String approve(BookingEntity booking, UUID approverUserId, OffsetDateTime now);

    String reject(BookingEntity booking, UUID approverUserId, String reason, OffsetDateTime now);
}
