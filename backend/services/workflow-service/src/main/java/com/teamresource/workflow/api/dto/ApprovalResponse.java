package com.teamresource.workflow.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ApprovalResponse(
        UUID approvalId,
        String targetType,
        UUID targetId,
        String approvalType,
        UUID requesterId,
        UUID approverId,
        UUID targetOwnerId,
        UUID resourceId,
        String title,
        String summary,
        int currentStep,
        int totalSteps,
        String approvalScope,
        String status,
        OffsetDateTime submittedAt,
        OffsetDateTime decidedAt,
        String decisionNote,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ApprovalStepResponse> steps,
        List<ApprovalHistoryResponse> history
) {
}
