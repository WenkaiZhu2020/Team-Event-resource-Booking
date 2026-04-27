package com.teamresource.analytics.service.facade;

import com.teamresource.analytics.api.dto.DashboardOverviewResponse;
import com.teamresource.analytics.api.dto.ResourcePopularityResponse;
import com.teamresource.analytics.service.DashboardOverviewBuilder;
import com.teamresource.analytics.service.DashboardQueryService;
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

    public DashboardOverviewResponse overview() {
        CompletableFuture<Long> totalBookings = CompletableFuture.supplyAsync(dashboardQueryService::totalBookings, analyticsTaskExecutor);
        CompletableFuture<Long> approvedBookings = CompletableFuture.supplyAsync(dashboardQueryService::approvedBookings, analyticsTaskExecutor);
        CompletableFuture<Long> pendingApprovals = CompletableFuture.supplyAsync(dashboardQueryService::pendingApprovalBookings, analyticsTaskExecutor);
        CompletableFuture<Long> waitlistedBookings = CompletableFuture.supplyAsync(dashboardQueryService::waitlistedBookings, analyticsTaskExecutor);
        CompletableFuture<Long> cancelledOrRejected = CompletableFuture.supplyAsync(dashboardQueryService::cancelledOrRejectedBookings, analyticsTaskExecutor);
        CompletableFuture<Long> uniqueResources = CompletableFuture.supplyAsync(dashboardQueryService::uniqueResourcesUsed, analyticsTaskExecutor);
        CompletableFuture<Long> upcomingApproved = CompletableFuture.supplyAsync(dashboardQueryService::nextSevenDaysApprovedBookings, analyticsTaskExecutor);
        CompletableFuture<Long> approvedMinutes = CompletableFuture.supplyAsync(dashboardQueryService::totalApprovedReservedMinutes, analyticsTaskExecutor);

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

    public List<ResourcePopularityResponse> topResources(int limit) {
        return dashboardQueryService.topResources(limit).stream()
                .map(entity -> new ResourcePopularityResponse(
                        entity.getResourceId(),
                        entity.getResourceName(),
                        entity.getResourceType(),
                        entity.getTotalBookings(),
                        entity.getApprovedBookings(),
                        entity.getPendingBookings(),
                        entity.getWaitlistedBookings(),
                        entity.getCancelledBookings(),
                        entity.getTotalReservedMinutes(),
                        entity.getPopularityScore(),
                        entity.getLastRefreshedAt()
                ))
                .toList();
    }
}
