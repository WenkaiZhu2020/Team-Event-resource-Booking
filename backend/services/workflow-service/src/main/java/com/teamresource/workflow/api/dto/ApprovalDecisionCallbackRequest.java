package com.teamresource.workflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalDecisionCallbackRequest(
        @NotBlank String decision,
        @Size(max = 400) String note
) {
}
