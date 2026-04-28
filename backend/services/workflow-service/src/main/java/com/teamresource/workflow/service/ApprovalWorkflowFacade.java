package com.teamresource.workflow.service;

import com.teamresource.workflow.api.dto.ApprovalResponse;
import com.teamresource.workflow.service.command.ApproveApprovalCommand;
import com.teamresource.workflow.service.command.CreateApprovalCommand;
import com.teamresource.workflow.service.command.RejectApprovalCommand;
import com.teamresource.workflow.service.handler.ApproveApprovalCommandHandler;
import com.teamresource.workflow.service.handler.CreateApprovalCommandHandler;
import com.teamresource.workflow.service.handler.RejectApprovalCommandHandler;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ApprovalWorkflowFacade {

    private final ApprovalService approvalService;
    private final CreateApprovalCommandHandler createApprovalCommandHandler;
    private final ApproveApprovalCommandHandler approveApprovalCommandHandler;
    private final RejectApprovalCommandHandler rejectApprovalCommandHandler;

    public ApprovalWorkflowFacade(
            ApprovalService approvalService,
            CreateApprovalCommandHandler createApprovalCommandHandler,
            ApproveApprovalCommandHandler approveApprovalCommandHandler,
            RejectApprovalCommandHandler rejectApprovalCommandHandler
    ) {
        this.approvalService = approvalService;
        this.createApprovalCommandHandler = createApprovalCommandHandler;
        this.approveApprovalCommandHandler = approveApprovalCommandHandler;
        this.rejectApprovalCommandHandler = rejectApprovalCommandHandler;
    }

    public ApprovalResponse create(CreateApprovalCommand command) {
        return createApprovalCommandHandler.handle(command);
    }

    public List<ApprovalResponse> pending(UUID currentUserId, boolean admin) {
        return approvalService.pending(currentUserId, admin);
    }

    public List<ApprovalResponse> requested(UUID requesterId) {
        return approvalService.requested(requesterId);
    }

    public ApprovalResponse byId(UUID approvalId, UUID currentUserId, boolean admin) {
        return approvalService.byId(approvalId, currentUserId, admin);
    }

    public ApprovalResponse approve(ApproveApprovalCommand command) {
        return approveApprovalCommandHandler.handle(command);
    }

    public ApprovalResponse reject(RejectApprovalCommand command) {
        return rejectApprovalCommandHandler.handle(command);
    }
}
