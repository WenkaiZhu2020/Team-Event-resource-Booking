package com.teamresource.workflow.api.dto;

import com.teamresource.workflow.domain.ApprovalTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateApprovalRequest(
        @NotNull ApprovalTargetType targetType,
        @NotNull UUID targetId,
        @NotNull UUID requesterId,
        UUID approverId,
        UUID targetOwnerId,
        UUID resourceId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 64) String approvalType,
        @Size(max = 1000) String summary,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        List<UUID> additionalApproverIds
) {
}
