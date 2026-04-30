package com.teamresource.analytics.api;

import com.teamresource.analytics.api.dto.ApiResponse;
import com.teamresource.analytics.api.dto.DashboardOverviewResponse;
import com.teamresource.analytics.api.dto.EventRegistrationMetricResponse;
import com.teamresource.analytics.api.dto.ResourcePopularityResponse;
import com.teamresource.analytics.api.dto.ResourceUsageMetricResponse;
import com.teamresource.analytics.service.facade.DashboardFacade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping({"/api/v1/dashboard", "/api/v1/analytics/dashboard"})
public class DashboardController {

    private final DashboardFacade dashboardFacade;

    public DashboardController(DashboardFacade dashboardFacade) {
        this.dashboardFacade = dashboardFacade;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('USER','ORGANIZER','RESOURCE_MANAGER','ADMIN')")
    public ApiResponse<DashboardOverviewResponse> overview(
            @RequestParam(name = "from", required = false) OffsetDateTime from,
            @RequestParam(name = "to", required = false) OffsetDateTime to
    ) {
        return ApiResponse.of(dashboardFacade.overview(from, to));
    }

    @GetMapping("/events/registrations")
    @PreAuthorize("hasAnyRole('USER','ORGANIZER','RESOURCE_MANAGER','ADMIN')")
    public ApiResponse<List<EventRegistrationMetricResponse>> eventRegistrations(
            @RequestParam(name = "from", required = false) OffsetDateTime from,
            @RequestParam(name = "to", required = false) OffsetDateTime to,
            @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(200) int limit
    ) {
        return ApiResponse.of(dashboardFacade.eventRegistrations(from, to, limit));
    }

    @GetMapping("/resources/usage")
    @PreAuthorize("hasAnyRole('USER','ORGANIZER','RESOURCE_MANAGER','ADMIN')")
    public ApiResponse<List<ResourceUsageMetricResponse>> resourceUsage(
            @RequestParam(name = "from", required = false) OffsetDateTime from,
            @RequestParam(name = "to", required = false) OffsetDateTime to,
            @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(200) int limit
    ) {
        return ApiResponse.of(dashboardFacade.resourceUsage(from, to, limit));
    }

    @GetMapping("/resources/popular")
    @PreAuthorize("hasAnyRole('USER','ORGANIZER','RESOURCE_MANAGER','ADMIN')")
    public ApiResponse<List<ResourcePopularityResponse>> topResources(
            @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(200) int limit
    ) {
        return ApiResponse.of(dashboardFacade.popularResources(limit));
    }
}
