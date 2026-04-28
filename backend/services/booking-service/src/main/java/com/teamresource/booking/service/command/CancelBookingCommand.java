package com.teamresource.booking.service.command;

import java.util.UUID;

public record CancelBookingCommand(
        UUID bookingId,
        UUID currentUserId,
        boolean admin
) {
}
