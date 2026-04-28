package com.teamresource.workflow.api;

import com.teamresource.workflow.api.dto.ApiResponse;
import com.teamresource.workflow.api.dto.ApprovalResponse;
import com.teamresource.workflow.api.dto.CreateApprovalRequest;
import com.teamresource.workflow.service.ApprovalWorkflowFacade;
import com.teamresource.workflow.service.command.CreateApprovalCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/workflows/approvals")
public class WorkflowInternalController {

    private final ApprovalWorkflowFacade approvalWorkflowFacade;

    public WorkflowInternalController(ApprovalWorkflowFacade approvalWorkflowFacade) {
        this.approvalWorkflowFacade = approvalWorkflowFacade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApprovalResponse> create(@Valid @RequestBody CreateApprovalRequest request) {
        return ApiResponse.of(approvalWorkflowFacade.create(new CreateApprovalCommand(
                request.targetType(),
                request.targetId(),
                request.requesterId(),
                request.approverId(),
                request.targetOwnerId(),
                request.resourceId(),
                request.title(),
                request.approvalType(),
                request.summary(),
                request.startAt(),
                request.endAt(),
                request.additionalApproverIds()
        )));
    }
}
