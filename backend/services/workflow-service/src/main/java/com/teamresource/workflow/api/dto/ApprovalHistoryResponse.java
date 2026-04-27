package com.teamresource.workflow.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApprovalHistoryResponse(
        UUID decisionId,
        String action,
        UUID actorId,
        String note,
        OffsetDateTime actedAt,
        OffsetDateTime createdAt
) {
}
