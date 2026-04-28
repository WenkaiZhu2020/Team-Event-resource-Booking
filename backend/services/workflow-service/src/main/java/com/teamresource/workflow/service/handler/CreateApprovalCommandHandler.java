package com.teamresource.workflow.service.handler;

import com.teamresource.workflow.api.dto.ApprovalResponse;
import com.teamresource.workflow.service.ApprovalService;
import com.teamresource.workflow.service.command.CreateApprovalCommand;
import org.springframework.stereotype.Component;

@Component
public class CreateApprovalCommandHandler implements CommandHandler<CreateApprovalCommand, ApprovalResponse> {

    private final ApprovalService approvalService;

    public CreateApprovalCommandHandler(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Override
    public ApprovalResponse handle(CreateApprovalCommand command) {
        return approvalService.create(command);
    }
}
