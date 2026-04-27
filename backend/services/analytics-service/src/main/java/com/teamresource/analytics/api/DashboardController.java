package com.teamresource.analytics.api;

import com.teamresource.analytics.api.dto.ApiResponse;
import com.teamresource.analytics.api.dto.DashboardOverviewResponse;
import com.teamresource.analytics.api.dto.ResourcePopularityResponse;
import com.teamresource.analytics.service.facade.DashboardFacade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/analytics/dashboard")
public class DashboardController {

    private final DashboardFacade dashboardFacade;

    public DashboardController(DashboardFacade dashboardFacade) {
        this.dashboardFacade = dashboardFacade;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> overview() {
        return ApiResponse.of(dashboardFacade.overview());
    }

    @GetMapping("/resources/popular")
    public ApiResponse<List<ResourcePopularityResponse>> topResources(
            @RequestParam(name = "limit", defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.of(dashboardFacade.topResources(limit));
    }
}
