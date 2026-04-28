package com.teamresource.workflow.service.handler;

import com.teamresource.workflow.domain.ApprovalDecisionType;
import com.teamresource.workflow.service.ApprovalService;
import com.teamresource.workflow.service.command.ApproveApprovalCommand;
import org.springframework.stereotype.Component;

@Component
public class ApproveApprovalCommandHandler extends AbstractDecisionCommandHandler<ApproveApprovalCommand> {

    public ApproveApprovalCommandHandler(ApprovalService approvalService) {
        super(approvalService);
    }

    @Override
    protected ApprovalDecisionType decisionType() {
        return ApprovalDecisionType.APPROVE;
    }
}
