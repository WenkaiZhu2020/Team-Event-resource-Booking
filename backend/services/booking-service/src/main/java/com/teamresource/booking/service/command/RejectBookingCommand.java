package com.teamresource.booking.service.command;

import com.teamresource.booking.api.dto.BookingDecisionRequest;
import java.util.UUID;

public record RejectBookingCommand(
        UUID bookingId,
        UUID currentUserId,
        boolean admin,
        BookingDecisionRequest request
) {
}
