package com.teamresource.workflow.service.handler;

import com.teamresource.workflow.api.dto.ApprovalResponse;
import com.teamresource.workflow.domain.ApprovalDecisionType;
import com.teamresource.workflow.service.ApprovalService;
import com.teamresource.workflow.service.command.DecisionCommand;

public abstract class AbstractDecisionCommandHandler<T extends DecisionCommand> implements CommandHandler<T, ApprovalResponse> {

    private final ApprovalService approvalService;

    protected AbstractDecisionCommandHandler(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Override
    public ApprovalResponse handle(T command) {
        return approvalService.applyDecision(
                command.approvalId(),
                command.actorId(),
                command.admin(),
                command.note(),
                decisionType()
        );
    }

    protected abstract ApprovalDecisionType decisionType();
}
