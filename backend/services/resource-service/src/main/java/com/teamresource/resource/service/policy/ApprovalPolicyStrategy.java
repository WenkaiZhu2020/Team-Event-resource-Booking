package com.teamresource.resource.service.policy;

import com.teamresource.resource.domain.ApprovalMode;

public interface ApprovalPolicyStrategy {

    ApprovalMode supports();

    boolean requiresApproval();
}
