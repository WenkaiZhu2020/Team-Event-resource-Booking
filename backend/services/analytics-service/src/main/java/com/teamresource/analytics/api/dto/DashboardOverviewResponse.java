package com.teamresource.analytics.api.dto;

public record DashboardOverviewResponse(
        long totalBookings,
        long approvedBookings,
        long pendingApprovals,
        long waitlistedBookings,
        long cancelledOrRejectedBookings,
        long uniqueResourcesUsed,
        long nextSevenDaysApprovedBookings,
        long totalApprovedReservedMinutes
) {
}
