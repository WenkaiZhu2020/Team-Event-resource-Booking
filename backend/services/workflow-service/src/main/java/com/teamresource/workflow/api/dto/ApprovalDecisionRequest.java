package com.teamresource.workflow.api.dto;

import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(
        @Size(max = 400) String note
) {
}
