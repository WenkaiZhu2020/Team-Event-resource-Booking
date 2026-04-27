package com.teamresource.workflow.service;

import java.util.UUID;

public record ApprovalDecisionCommand(
        UUID approvalId,
        UUID actorId,
        boolean admin,
        String note
) {
}
