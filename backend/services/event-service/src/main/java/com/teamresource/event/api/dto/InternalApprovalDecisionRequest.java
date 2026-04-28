package com.teamresource.event.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InternalApprovalDecisionRequest(
        @NotBlank @Size(max = 32) String decision,
        @Size(max = 400) String note
) {
}
