package com.teamresource.booking.api.dto;

import jakarta.validation.constraints.Size;

public record BookingDecisionRequest(
        @Size(max = 400) String note
) {
}
