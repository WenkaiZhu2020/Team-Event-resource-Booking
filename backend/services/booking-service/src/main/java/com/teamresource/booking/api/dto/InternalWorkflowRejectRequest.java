package com.teamresource.booking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InternalWorkflowRejectRequest(
        @NotNull UUID approverUserId,
        @NotBlank @Size(max = 300) String reason
) {
}
