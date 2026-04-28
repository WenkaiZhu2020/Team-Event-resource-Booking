package com.teamresource.workflow.service.command;

import com.teamresource.workflow.domain.ApprovalTargetType;
import java.util.UUID;

public record CompleteApprovalCallbackCommand(
        UUID approvalId,
        ApprovalTargetType targetType,
        UUID targetId,
        String decision,
        String note
) {
}
