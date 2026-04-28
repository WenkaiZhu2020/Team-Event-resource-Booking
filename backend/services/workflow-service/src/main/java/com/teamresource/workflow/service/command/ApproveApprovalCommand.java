package com.teamresource.workflow.service.command;

import java.util.UUID;

public record ApproveApprovalCommand(
        UUID approvalId,
        UUID actorId,
        boolean admin,
        String note
) implements DecisionCommand {
}
