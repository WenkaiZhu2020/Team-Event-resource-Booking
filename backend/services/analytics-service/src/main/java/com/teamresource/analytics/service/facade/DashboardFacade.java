package com.teamresource.analytics.service.facade;

import com.teamresource.analytics.api.dto.DashboardOverviewResponse;
import com.teamresource.analytics.api.dto.EventRegistrationMetricResponse;
import com.teamresource.analytics.api.dto.ResourcePopularityResponse;
import com.teamresource.analytics.api.dto.ResourceUsageMetricResponse;
import com.teamresource.analytics.service.DashboardOverviewBuilder;
import com.teamresource.analytics.service.DashboardQueryService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DashboardFacade {

    private final DashboardQueryService dashboardQueryService;
    private final Executor analyticsTaskExecutor;

    public DashboardFacade(
            DashboardQueryService dashboardQueryService,
            @Qualifier("analyticsTaskExecutor") Executor analyticsTaskExecutor
    ) {
        this.dashboardQueryService = dashboardQueryService;
        this.analyticsTaskExecutor = analyticsTaskExecutor;
    }

    public DashboardOverviewResponse overview(OffsetDateTime from, OffsetDateTime to) {
        CompletableFuture<Long> totalBookings = CompletableFuture.supplyAsync(() -> dashboardQueryService.totalBookings(from, to), analyticsTaskExecutor);
        CompletableFuture<Long> approvedBookings = CompletableFuture.supplyAsync(() -> dashboardQueryService.approvedBookings(from, to), analyticsTaskExecutor);
        CompletableFuture<Long> pendingApprovals = CompletableFuture.supplyAsync(() -> dashboardQueryService.pendingApprovalBookings(from, to), analyticsTaskExecutor);
        CompletableFuture<Long> waitlistedBookings = CompletableFuture.supplyAsync(() -> dashboardQueryService.waitlistedBookings(from, to), analyticsTaskExecutor);
        CompletableFuture<Long> cancelledOrRejected = CompletableFuture.supplyAsync(() -> dashboardQueryService.cancelledOrRejectedBookings(from, to), analyticsTaskExecutor);
        CompletableFuture<Long> uniqueResources = CompletableFuture.supplyAsync(() -> dashboardQueryService.uniqueResourcesUsed(from, to), analyticsTaskExecutor);
        CompletableFuture<Long> upcomingApproved = CompletableFuture.supplyAsync(() -> dashboardQueryService.nextSevenDaysApprovedBookings(from, to), analyticsTaskExecutor);
        CompletableFuture<Long> approvedMinutes = CompletableFuture.supplyAsync(() -> dashboardQueryService.totalApprovedReservedMinutes(from, to), analyticsTaskExecutor);

        CompletableFuture.allOf(
                totalBookings,
                approvedBookings,
                pendingApprovals,
                waitlistedBookings,
                cancelledOrRejected,
                uniqueResources,
                upcomingApproved,
                approvedMinutes
        ).join();

        return new DashboardOverviewBuilder()
                .totalBookings(totalBookings.join())
                .approvedBookings(approvedBookings.join())
                .pendingApprovals(pendingApprovals.join())
                .waitlistedBookings(waitlistedBookings.join())
                .cancelledOrRejectedBookings(cancelledOrRejected.join())
                .uniqueResourcesUsed(uniqueResources.join())
                .nextSevenDaysApprovedBookings(upcomingApproved.join())
                .totalApprovedReservedMinutes(approvedMinutes.join())
                .build();
    }

    public DashboardOverviewResponse overview() {
        return overview(null, null);
    }

    public List<EventRegistrationMetricResponse> eventRegistrations(OffsetDateTime from, OffsetDateTime to, int limit) {
        return dashboardQueryService.eventRegistrationMetrics(from, to, limit);
    }

    public List<ResourceUsageMetricResponse> resourceUsage(OffsetDateTime from, OffsetDateTime to, int limit) {
        return dashboardQueryService.resourceUsageMetrics(from, to, limit);
    }

    public List<ResourcePopularityResponse> popularResources(int limit) {
        return dashboardQueryService.topResources(limit);
    }

    public List<ResourcePopularityResponse> topResources(int limit) {
        return popularResources(limit);
    }
}
