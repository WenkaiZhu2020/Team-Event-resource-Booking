package com.teamresource.resource.service.policy;

import com.teamresource.resource.domain.ApprovalMode;
import org.springframework.stereotype.Component;

@Component
public class ManagerApprovalStrategy implements ApprovalPolicyStrategy {

    @Override
    public ApprovalMode supports() {
        return ApprovalMode.MANAGER_APPROVAL;
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }
}
