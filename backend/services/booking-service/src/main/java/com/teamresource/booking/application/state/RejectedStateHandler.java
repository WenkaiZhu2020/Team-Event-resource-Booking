package com.teamresource.booking.application.state;

import com.teamresource.booking.domain.model.BookingStatus;
import org.springframework.stereotype.Component;

@Component
public class RejectedStateHandler extends AbstractBookingStateHandler {

    @Override
    public BookingStatus supports() {
        return BookingStatus.REJECTED;
    }
}
