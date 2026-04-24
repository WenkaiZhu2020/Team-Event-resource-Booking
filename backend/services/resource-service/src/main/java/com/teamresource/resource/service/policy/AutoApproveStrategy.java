package com.teamresource.resource.service.policy;

import com.teamresource.resource.domain.ApprovalMode;
import org.springframework.stereotype.Component;

@Component
public class AutoApproveStrategy implements ApprovalPolicyStrategy {

    @Override
    public ApprovalMode supports() {
        return ApprovalMode.AUTO_APPROVE;
    }

    @Override
    public boolean requiresApproval() {
        return false;
    }
}
