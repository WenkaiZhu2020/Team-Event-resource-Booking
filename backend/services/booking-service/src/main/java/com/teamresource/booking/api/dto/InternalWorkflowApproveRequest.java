package com.teamresource.booking.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InternalWorkflowApproveRequest(
        @NotNull UUID approverUserId
) {
}
