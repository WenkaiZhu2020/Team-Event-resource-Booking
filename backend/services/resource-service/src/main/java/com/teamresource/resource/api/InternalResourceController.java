package com.teamresource.resource.api;

import com.teamresource.resource.api.dto.ApiResponse;
import com.teamresource.resource.api.dto.ResourceApprovalPolicyResponse;
import com.teamresource.resource.service.ResourceService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/resources")
public class InternalResourceController {

    private final ResourceService resourceService;

    public InternalResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/{resourceId}/approval-policy")
    public ApiResponse<ResourceApprovalPolicyResponse> approvalPolicy(@PathVariable UUID resourceId) {
        return ApiResponse.of(resourceService.approvalPolicy(resourceId));
    }
}
