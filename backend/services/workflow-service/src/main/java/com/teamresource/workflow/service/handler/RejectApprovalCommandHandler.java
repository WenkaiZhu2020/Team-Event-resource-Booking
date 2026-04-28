package com.teamresource.workflow.service.handler;

import com.teamresource.workflow.domain.ApprovalDecisionType;
import com.teamresource.workflow.service.ApprovalService;
import com.teamresource.workflow.service.command.RejectApprovalCommand;
import org.springframework.stereotype.Component;

@Component
public class RejectApprovalCommandHandler extends AbstractDecisionCommandHandler<RejectApprovalCommand> {

    public RejectApprovalCommandHandler(ApprovalService approvalService) {
        super(approvalService);
    }

    @Override
    protected ApprovalDecisionType decisionType() {
        return ApprovalDecisionType.REJECT;
    }
}
