package com.teamresource.analytics.api;

import com.teamresource.analytics.api.dto.ApiResponse;
import com.teamresource.analytics.service.ResourcePopularityRefreshService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/analytics/admin")
public class AnalyticsAdminController {

    private final ResourcePopularityRefreshService resourcePopularityRefreshService;

    public AnalyticsAdminController(ResourcePopularityRefreshService resourcePopularityRefreshService) {
        this.resourcePopularityRefreshService = resourcePopularityRefreshService;
    }

    @PostMapping("/resource-popularity/refresh")
    public ApiResponse<String> refresh(Authentication authentication) {
        if (!hasRole(authentication, "ROLE_ADMIN")) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Admin access required");
        }
        resourcePopularityRefreshService.refresh();
        return ApiResponse.of("Resource popularity refresh completed");
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
