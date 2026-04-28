package com.teamresource.booking.application.command;

import java.util.UUID;

public record CancelBookingCommand(
        UUID bookingId,
        UUID actorUserId,
        boolean admin,
        String reason
) {
}
