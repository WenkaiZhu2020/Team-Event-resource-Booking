package com.teamresource.analytics.service;

import com.teamresource.analytics.api.dto.DashboardOverviewResponse;
import com.teamresource.analytics.api.dto.EventRegistrationMetricResponse;
import com.teamresource.analytics.api.dto.ResourcePopularityResponse;
import com.teamresource.analytics.api.dto.ResourceUsageMetricResponse;
import com.teamresource.analytics.service.facade.DashboardFacade;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
class DashboardFacadeTest {

    private final Executor directExecutor = Runnable::run;

    @Test
    void overviewShouldAggregateAllMetrics() {
        StubDashboardQueryService dashboardQueryService = new StubDashboardQueryService();

        DashboardFacade facade = new DashboardFacade(dashboardQueryService, directExecutor);

        DashboardOverviewResponse response = facade.overview();

        assertThat(response.totalBookings()).isEqualTo(12L);
        assertThat(response.approvedBookings()).isEqualTo(7L);
        assertThat(response.pendingApprovals()).isEqualTo(2L);
        assertThat(response.totalApprovedReservedMinutes()).isEqualTo(840L);
    }

    @Test
    void filteredQueriesShouldDelegate() {
        StubDashboardQueryService dashboardQueryService = new StubDashboardQueryService();
        DashboardFacade facade = new DashboardFacade(dashboardQueryService, directExecutor);
        OffsetDateTime from = OffsetDateTime.parse("2026-04-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-04-30T23:59:59Z");

        List<EventRegistrationMetricResponse> eventRegistrations =
                List.of(new EventRegistrationMetricResponse(UUID.randomUUID(), 2, 1, 0));
        List<ResourceUsageMetricResponse> resourceUsage =
                List.of(new ResourceUsageMetricResponse(UUID.randomUUID(), 3, 2, 1, 0, 180));
        List<ResourcePopularityResponse> popularResources = List.of(new ResourcePopularityResponse(
                UUID.randomUUID(),
                "Desk A",
                "DESK",
                3,
                2,
                1,
                0,
                0,
                180,
                BigDecimal.TEN,
                OffsetDateTime.parse("2026-04-30T12:00:00Z")));

        dashboardQueryService.eventRegistrationResponses = eventRegistrations;
        dashboardQueryService.resourceUsageResponses = resourceUsage;
        dashboardQueryService.popularResourcesResponses = popularResources;

        assertThat(facade.eventRegistrations(from, to, 10)).containsExactlyElementsOf(eventRegistrations);
        assertThat(facade.resourceUsage(from, to, 10)).containsExactlyElementsOf(resourceUsage);
        assertThat(facade.popularResources(10)).containsExactlyElementsOf(popularResources);
        assertThat(facade.topResources(10)).containsExactlyElementsOf(popularResources);
    }

    static class StubDashboardQueryService extends DashboardQueryService {

        private List<EventRegistrationMetricResponse> eventRegistrationResponses = List.of();
        private List<ResourceUsageMetricResponse> resourceUsageResponses = List.of();
        private List<ResourcePopularityResponse> popularResourcesResponses = List.of();

        StubDashboardQueryService() {
            super(null, null);
        }

        @Override public long totalBookings(OffsetDateTime from, OffsetDateTime to) { return 12L; }
        @Override public long approvedBookings(OffsetDateTime from, OffsetDateTime to) { return 7L; }
        @Override public long pendingApprovalBookings(OffsetDateTime from, OffsetDateTime to) { return 2L; }
        @Override public long waitlistedBookings(OffsetDateTime from, OffsetDateTime to) { return 1L; }
        @Override public long cancelledOrRejectedBookings(OffsetDateTime from, OffsetDateTime to) { return 2L; }
        @Override public long uniqueResourcesUsed(OffsetDateTime from, OffsetDateTime to) { return 5L; }
        @Override public long nextSevenDaysApprovedBookings(OffsetDateTime from, OffsetDateTime to) { return 3L; }
        @Override public long totalApprovedReservedMinutes(OffsetDateTime from, OffsetDateTime to) { return 840L; }
        @Override public List<EventRegistrationMetricResponse> eventRegistrationMetrics(OffsetDateTime from, OffsetDateTime to, int limit) { return eventRegistrationResponses; }
        @Override public List<ResourceUsageMetricResponse> resourceUsageMetrics(OffsetDateTime from, OffsetDateTime to, int limit) { return resourceUsageResponses; }
        @Override public List<ResourcePopularityResponse> topResources(int limit) { return popularResourcesResponses; }
    }
}
