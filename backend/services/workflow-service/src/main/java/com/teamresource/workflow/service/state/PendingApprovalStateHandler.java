package com.teamresource.workflow.service.state;

import com.teamresource.workflow.domain.ApprovalDecisionType;
import com.teamresource.workflow.domain.ApprovalStatus;
import org.springframework.stereotype.Component;

@Component
public class PendingApprovalStateHandler implements ApprovalStateHandler {

    @Override
    public ApprovalStatus supportedStatus() {
        return ApprovalStatus.PENDING;
    }

    @Override
    public ApprovalStatus transition(ApprovalDecisionType decisionType) {
        return switch (decisionType) {
            case APPROVE -> ApprovalStatus.APPROVED;
            case REJECT -> ApprovalStatus.REJECTED;
        };
    }
}
