package com.teamresource.booking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelBookingRequest(
        @NotBlank @Size(max = 300) String reason
) {
}
