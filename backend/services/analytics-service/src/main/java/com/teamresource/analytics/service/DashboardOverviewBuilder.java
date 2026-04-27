package com.teamresource.analytics.service;

import com.teamresource.analytics.api.dto.DashboardOverviewResponse;

public class DashboardOverviewBuilder {

    private long totalBookings;
    private long approvedBookings;
    private long pendingApprovals;
    private long waitlistedBookings;
    private long cancelledOrRejectedBookings;
    private long uniqueResourcesUsed;
    private long nextSevenDaysApprovedBookings;
    private long totalApprovedReservedMinutes;

    public DashboardOverviewBuilder totalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
        return this;
    }

    public DashboardOverviewBuilder approvedBookings(long approvedBookings) {
        this.approvedBookings = approvedBookings;
        return this;
    }

    public DashboardOverviewBuilder pendingApprovals(long pendingApprovals) {
        this.pendingApprovals = pendingApprovals;
        return this;
    }

    public DashboardOverviewBuilder waitlistedBookings(long waitlistedBookings) {
        this.waitlistedBookings = waitlistedBookings;
        return this;
    }

    public DashboardOverviewBuilder cancelledOrRejectedBookings(long cancelledOrRejectedBookings) {
        this.cancelledOrRejectedBookings = cancelledOrRejectedBookings;
        return this;
    }

    public DashboardOverviewBuilder uniqueResourcesUsed(long uniqueResourcesUsed) {
        this.uniqueResourcesUsed = uniqueResourcesUsed;
        return this;
    }

    public DashboardOverviewBuilder nextSevenDaysApprovedBookings(long nextSevenDaysApprovedBookings) {
        this.nextSevenDaysApprovedBookings = nextSevenDaysApprovedBookings;
        return this;
    }

    public DashboardOverviewBuilder totalApprovedReservedMinutes(long totalApprovedReservedMinutes) {
        this.totalApprovedReservedMinutes = totalApprovedReservedMinutes;
        return this;
    }

    public DashboardOverviewResponse build() {
        return new DashboardOverviewResponse(
                totalBookings,
                approvedBookings,
                pendingApprovals,
                waitlistedBookings,
                cancelledOrRejectedBookings,
                uniqueResourcesUsed,
                nextSevenDaysApprovedBookings,
                totalApprovedReservedMinutes
        );
    }
}
