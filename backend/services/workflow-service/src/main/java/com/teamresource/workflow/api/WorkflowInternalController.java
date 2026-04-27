package com.teamresource.workflow.api;

import com.teamresource.workflow.api.dto.ApiResponse;
import com.teamresource.workflow.api.dto.ApprovalResponse;
import com.teamresource.workflow.api.dto.CreateApprovalRequest;
import com.teamresource.workflow.service.ApprovalService;
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

    private final ApprovalService approvalService;

    public WorkflowInternalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApprovalResponse> create(@Valid @RequestBody CreateApprovalRequest request) {
        return ApiResponse.of(approvalService.create(request));
    }
}
