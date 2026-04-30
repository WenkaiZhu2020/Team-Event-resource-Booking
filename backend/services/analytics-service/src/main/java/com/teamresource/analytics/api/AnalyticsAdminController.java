package com.teamresource.analytics.api;

import com.teamresource.analytics.api.dto.ApiResponse;
import com.teamresource.analytics.service.ResourcePopularityRefreshService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsAdminController {

    private final ResourcePopularityRefreshService resourcePopularityRefreshService;

    public AnalyticsAdminController(ResourcePopularityRefreshService resourcePopularityRefreshService) {
        this.resourcePopularityRefreshService = resourcePopularityRefreshService;
    }

    @PostMapping("/admin/resource-popularity/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> refresh() {
        resourcePopularityRefreshService.refresh();
        return ApiResponse.of("ok");
    }
}
