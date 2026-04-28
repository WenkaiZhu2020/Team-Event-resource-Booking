package com.teamresource.booking.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID resourceId,
        @JsonAlias("eventId") UUID linkedEventId,
        @NotNull @Future OffsetDateTime startAt,
        @NotNull @Future OffsetDateTime endAt,
        @NotBlank String purpose
) {
    public UUID eventId() {
        return linkedEventId;
    }
}
