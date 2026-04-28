package com.teamresource.booking.application.state;

import com.teamresource.booking.domain.model.BookingStatus;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class WaitlistedStateHandler extends AbstractBookingStateHandler {

    @Override
    public BookingStatus supports() {
        return BookingStatus.WAITLISTED;
    }

    @Override
    public String cancel(BookingEntity booking, String reason, OffsetDateTime now) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        booking.setCancellationReason(reason);
        return "BOOKING_WAITLIST_CANCELLED";
    }
}
