package com.teamresource.analytics.service;

import com.teamresource.analytics.domain.BookingAnalyticsStatus;
import com.teamresource.analytics.infra.persistence.BookingFactRepository;
import com.teamresource.analytics.infra.persistence.ResourcePopularityEntity;
import com.teamresource.analytics.infra.persistence.ResourcePopularityRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DashboardQueryService {

    private final BookingFactRepository bookingFactRepository;
    private final ResourcePopularityRepository resourcePopularityRepository;

    public DashboardQueryService(
            BookingFactRepository bookingFactRepository,
            ResourcePopularityRepository resourcePopularityRepository
    ) {
        this.bookingFactRepository = bookingFactRepository;
        this.resourcePopularityRepository = resourcePopularityRepository;
    }

    public long totalBookings() {
        return bookingFactRepository.count();
    }

    public long approvedBookings() {
        return bookingFactRepository.countByBookingStatus(BookingAnalyticsStatus.APPROVED);
    }

    public long pendingApprovalBookings() {
        return bookingFactRepository.countByBookingStatus(BookingAnalyticsStatus.PENDING_APPROVAL);
    }

    public long waitlistedBookings() {
        return bookingFactRepository.countByBookingStatus(BookingAnalyticsStatus.WAITLISTED);
    }

    public long cancelledOrRejectedBookings() {
        return bookingFactRepository.countByBookingStatusIn(List.of(BookingAnalyticsStatus.CANCELLED, BookingAnalyticsStatus.REJECTED));
    }

    public long uniqueResourcesUsed() {
        return bookingFactRepository.countDistinctResourcesUsed(List.of(
                BookingAnalyticsStatus.APPROVED,
                BookingAnalyticsStatus.PENDING_APPROVAL,
                BookingAnalyticsStatus.WAITLISTED,
                BookingAnalyticsStatus.CANCELLED,
                BookingAnalyticsStatus.REJECTED
        ));
    }

    public long nextSevenDaysApprovedBookings() {
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC);
        return bookingFactRepository.countApprovedBookingsBetween(from, from.plusDays(7));
    }

    public long totalApprovedReservedMinutes() {
        return bookingFactRepository.sumApprovedReservedMinutes();
    }

    public List<ResourcePopularityEntity> topResources(int limit) {
        return resourcePopularityRepository.findAllByOrderByPopularityScoreDesc(PageRequest.of(0, limit));
    }
}
