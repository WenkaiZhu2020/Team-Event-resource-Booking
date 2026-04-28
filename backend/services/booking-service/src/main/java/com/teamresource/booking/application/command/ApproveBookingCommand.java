package com.teamresource.booking.application.command;

import java.util.UUID;

public record ApproveBookingCommand(
        UUID bookingId,
        UUID approverUserId
) {
}
