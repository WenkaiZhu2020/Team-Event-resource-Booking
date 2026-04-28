package com.teamresource.workflow.infra.client;

import java.util.UUID;

public interface ResourceApprovalPolicyGateway {

    ResourceApprovalPolicy resolve(UUID resourceId);

    record ResourceApprovalPolicy(
            UUID resourceId,
            UUID managerId,
            String approvalMode,
            boolean requiresApproval
    ) {
    }
}
