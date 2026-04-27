package com.teamresource.booking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InternalBookingDecisionRequest(
        @NotBlank String decision,
        @Size(max = 400) String note
) {
}
