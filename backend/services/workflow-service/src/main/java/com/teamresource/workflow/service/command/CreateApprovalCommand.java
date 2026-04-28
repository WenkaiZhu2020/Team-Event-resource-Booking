package com.teamresource.workflow.service.command;

import com.teamresource.workflow.domain.ApprovalTargetType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateApprovalCommand(
        ApprovalTargetType targetType,
        UUID targetId,
        UUID requesterId,
        UUID approverId,
        UUID targetOwnerId,
        UUID resourceId,
        String title,
        String approvalType,
        String summary,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        List<UUID> additionalApproverIds
) {
}
