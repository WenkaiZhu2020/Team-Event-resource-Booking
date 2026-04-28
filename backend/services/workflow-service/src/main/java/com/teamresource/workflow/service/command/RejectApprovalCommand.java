package com.teamresource.workflow.service.command;

import java.util.UUID;

public record RejectApprovalCommand(
        UUID approvalId,
        UUID actorId,
        boolean admin,
        String note
) implements DecisionCommand {
}
