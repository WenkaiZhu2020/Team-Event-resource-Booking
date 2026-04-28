package com.teamresource.workflow.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApprovalStepResponse(
        UUID stepId,
        int stepNumber,
        UUID approverId,
        String status,
        String decisionNote,
        OffsetDateTime decidedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
