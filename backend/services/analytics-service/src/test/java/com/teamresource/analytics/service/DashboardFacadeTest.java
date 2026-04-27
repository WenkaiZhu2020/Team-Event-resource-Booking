package com.teamresource.analytics.service;

import com.teamresource.analytics.api.dto.DashboardOverviewResponse;
import com.teamresource.analytics.service.facade.DashboardFacade;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardFacadeTest {

    private final Executor directExecutor = Runnable::run;

    @Test
    void overviewShouldAggregateAllMetrics() {
        DashboardQueryService dashboardQueryService = new StubDashboardQueryService();
        DashboardFacade facade = new DashboardFacade(dashboardQueryService, directExecutor);

        DashboardOverviewResponse response = facade.overview();

        assertThat(response.totalBookings()).isEqualTo(12L);
        assertThat(response.approvedBookings()).isEqualTo(7L);
        assertThat(response.pendingApprovals()).isEqualTo(2L);
        assertThat(response.totalApprovedReservedMinutes()).isEqualTo(840L);
    }

    static class StubDashboardQueryService extends DashboardQueryService {

        StubDashboardQueryService() {
            super(null, null);
        }

        @Override
        public long totalBookings() {
            return 12L;
        }

        @Override
        public long approvedBookings() {
            return 7L;
        }

        @Override
        public long pendingApprovalBookings() {
            return 2L;
        }

        @Override
        public long waitlistedBookings() {
            return 1L;
        }

        @Override
        public long cancelledOrRejectedBookings() {
            return 2L;
        }

        @Override
        public long uniqueResourcesUsed() {
            return 5L;
        }

        @Override
        public long nextSevenDaysApprovedBookings() {
            return 3L;
        }

        @Override
        public long totalApprovedReservedMinutes() {
            return 840L;
        }
    }
}
