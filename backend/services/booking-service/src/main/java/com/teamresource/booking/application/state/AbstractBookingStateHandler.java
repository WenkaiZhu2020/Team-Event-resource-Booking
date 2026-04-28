package com.teamresource.booking.application.state;

import com.teamresource.booking.common.error.ApiException;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public abstract class AbstractBookingStateHandler implements BookingStateHandler {

    protected ApiException transitionError(String action, BookingEntity booking) {
        return new ApiException(
                "BOOKING_STATE_TRANSITION_NOT_ALLOWED",
                HttpStatus.CONFLICT,
                "Cannot " + action + " booking in state " + booking.getStatus()
        );
    }

    @Override
    public String approve(BookingEntity booking, UUID approverUserId, OffsetDateTime now) {
        throw transitionError("approve", booking);
    }

    @Override
    public String reject(BookingEntity booking, UUID approverUserId, String reason, OffsetDateTime now) {
        throw transitionError("reject", booking);
    }

    @Override
    public String cancel(BookingEntity booking, String reason, OffsetDateTime now) {
        throw transitionError("cancel", booking);
    }
}
