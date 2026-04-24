package com.teamresource.event.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record CreateEventRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 4000) String description,
        @NotBlank String category,
        @NotBlank @Size(max = 160) String location,
        @Min(1) int capacity,
        OffsetDateTime registrationOpenAt,
        OffsetDateTime registrationCloseAt,
        @NotNull @Future OffsetDateTime startAt,
        @NotNull @Future OffsetDateTime endAt
) {
}
