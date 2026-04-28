package com.teamresource.booking.application.command;

import java.util.UUID;

public record RejectBookingCommand(
        UUID bookingId,
        UUID approverUserId,
        String reason
) {
}
