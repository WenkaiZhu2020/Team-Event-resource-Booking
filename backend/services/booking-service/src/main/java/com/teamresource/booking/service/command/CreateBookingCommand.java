package com.teamresource.booking.service.command;

import com.teamresource.booking.api.dto.CreateBookingRequest;
import java.util.UUID;

public record CreateBookingCommand(
        UUID userId,
        CreateBookingRequest request,
        String idempotencyKey
) {
}
