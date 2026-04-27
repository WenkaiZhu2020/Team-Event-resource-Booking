package com.teamresource.workflow.api;

import com.teamresource.workflow.api.dto.ApiResponse;
import com.teamresource.workflow.api.dto.ApprovalDecisionRequest;
import com.teamresource.workflow.api.dto.ApprovalResponse;
import com.teamresource.workflow.service.ApprovalDecisionCommand;
import com.teamresource.workflow.service.ApprovalService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/workflows/approvals")
public class WorkflowController {

    private final ApprovalService approvalService;

    public WorkflowController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/pending")
    public ApiResponse<List<ApprovalResponse>> pending(Principal principal, Authentication authentication) {
        return ApiResponse.of(approvalService.pending(parsePrincipal(principal), hasRole(authentication, "ROLE_ADMIN")));
    }

    @GetMapping("/requested")
    public ApiResponse<List<ApprovalResponse>> requested(Principal principal) {
        return ApiResponse.of(approvalService.requested(parsePrincipal(principal)));
    }

    @GetMapping("/{approvalId}")
    public ApiResponse<ApprovalResponse> byId(@PathVariable UUID approvalId, Principal principal, Authentication authentication) {
        return ApiResponse.of(approvalService.byId(approvalId, parsePrincipal(principal), hasRole(authentication, "ROLE_ADMIN")));
    }

    @PostMapping("/{approvalId}/approve")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ApprovalResponse> approve(
            @PathVariable UUID approvalId,
            Principal principal,
            Authentication authentication,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest request
    ) {
        ApprovalDecisionRequest safeRequest = request == null ? new ApprovalDecisionRequest(null) : request;
        return ApiResponse.of(approvalService.approve(new ApprovalDecisionCommand(
                approvalId,
                parsePrincipal(principal),
                hasRole(authentication, "ROLE_ADMIN"),
                safeRequest.note()
        )));
    }

    @PostMapping("/{approvalId}/reject")
    public ApiResponse<ApprovalResponse> reject(
            @PathVariable UUID approvalId,
            Principal principal,
            Authentication authentication,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest request
    ) {
        ApprovalDecisionRequest safeRequest = request == null ? new ApprovalDecisionRequest(null) : request;
        return ApiResponse.of(approvalService.reject(new ApprovalDecisionCommand(
                approvalId,
                parsePrincipal(principal),
                hasRole(authentication, "ROLE_ADMIN"),
                safeRequest.note()
        )));
    }

    private UUID parsePrincipal(Principal principal) {
        try {
            return UUID.fromString(principal.getName());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
