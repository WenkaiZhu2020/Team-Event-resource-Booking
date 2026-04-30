package com.teamresource.analytics.service;

import com.teamresource.analytics.api.dto.EventRegistrationMetricResponse;
import com.teamresource.analytics.api.dto.ResourcePopularityResponse;
import com.teamresource.analytics.api.dto.ResourceUsageMetricResponse;
import com.teamresource.analytics.domain.BookingAnalyticsStatus;
import com.teamresource.analytics.infra.persistence.BookingFactRepository;
import com.teamresource.analytics.infra.persistence.ResourcePopularityRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DashboardQueryService {
    private static final OffsetDateTime MIN_WINDOW = OffsetDateTime.parse("2000-01-01T00:00:00Z");
    private static final OffsetDateTime MAX_WINDOW = OffsetDateTime.parse("2100-01-01T00:00:00Z");

    private final BookingFactRepository bookingFactRepository;
    private final ResourcePopularityRepository resourcePopularityRepository;

    public DashboardQueryService(
            BookingFactRepository bookingFactRepository,
            ResourcePopularityRepository resourcePopularityRepository
    ) {
        this.bookingFactRepository = bookingFactRepository;
        this.resourcePopularityRepository = resourcePopularityRepository;
    }

    public long totalBookings(OffsetDateTime from, OffsetDateTime to) {
        return bookingFactRepository.countAll(effectiveFrom(from), effectiveTo(to));
    }

    public long approvedBookings(OffsetDateTime from, OffsetDateTime to) {
        return bookingFactRepository.countByBookingStatus(BookingAnalyticsStatus.APPROVED, effectiveFrom(from), effectiveTo(to));
    }

    public long pendingApprovalBookings(OffsetDateTime from, OffsetDateTime to) {
        return bookingFactRepository.countByBookingStatus(BookingAnalyticsStatus.PENDING_APPROVAL, effectiveFrom(from), effectiveTo(to));
    }

    public long waitlistedBookings(OffsetDateTime from, OffsetDateTime to) {
        return bookingFactRepository.countByBookingStatus(BookingAnalyticsStatus.WAITLISTED, effectiveFrom(from), effectiveTo(to));
    }

    public long cancelledOrRejectedBookings(OffsetDateTime from, OffsetDateTime to) {
        return bookingFactRepository.countByBookingStatusIn(
                List.of(BookingAnalyticsStatus.CANCELLED, BookingAnalyticsStatus.REJECTED),
                effectiveFrom(from),
                effectiveTo(to)
        );
    }

    public long uniqueResourcesUsed(OffsetDateTime from, OffsetDateTime to) {
        return bookingFactRepository.countDistinctResourcesUsed(List.of(
                BookingAnalyticsStatus.APPROVED,
                BookingAnalyticsStatus.PENDING_APPROVAL,
                BookingAnalyticsStatus.WAITLISTED,
                BookingAnalyticsStatus.CANCELLED,
                BookingAnalyticsStatus.REJECTED
        ), effectiveFrom(from), effectiveTo(to));
    }

    public long nextSevenDaysApprovedBookings(OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime effectiveFrom = from == null ? OffsetDateTime.now(ZoneOffset.UTC) : from;
        OffsetDateTime effectiveTo = to == null ? effectiveFrom.plusDays(7) : to;
        return bookingFactRepository.countApprovedBookingsBetween(effectiveFrom, effectiveTo);
    }

    public long totalApprovedReservedMinutes(OffsetDateTime from, OffsetDateTime to) {
        return bookingFactRepository.sumApprovedReservedMinutes(effectiveFrom(from), effectiveTo(to));
    }

    public List<EventRegistrationMetricResponse> eventRegistrationMetrics(OffsetDateTime from, OffsetDateTime to, int limit) {
        return bookingFactRepository.aggregateEventRegistrations(effectiveFrom(from), effectiveTo(to), limit).stream()
                .map(projection -> new EventRegistrationMetricResponse(
                        projection.getEventId(),
                        projection.getActiveBookings(),
                        projection.getWaitlistedBookings(),
                        projection.getCancelledBookings()
                ))
                .toList();
    }

    public List<ResourceUsageMetricResponse> resourceUsageMetrics(OffsetDateTime from, OffsetDateTime to, int limit) {
        return bookingFactRepository.aggregateResourceUsage(effectiveFrom(from), effectiveTo(to), limit).stream()
                .map(projection -> new ResourceUsageMetricResponse(
                        projection.getResourceId(),
                        projection.getTotalBookings(),
                        projection.getApprovedBookings(),
                        projection.getPendingBookings(),
                        projection.getCancelledBookings(),
                        projection.getBookedMinutes()
                ))
                .toList();
    }

    public List<ResourcePopularityResponse> topResources(int limit) {
        return resourcePopularityRepository.findAllByOrderByPopularityScoreDesc(PageRequest.of(0, limit)).stream()
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

    public long totalBookings() {
        return totalBookings(null, null);
    }

    public long approvedBookings() {
        return approvedBookings(null, null);
    }

    public long pendingApprovalBookings() {
        return pendingApprovalBookings(null, null);
    }

    public long waitlistedBookings() {
        return waitlistedBookings(null, null);
    }

    public long cancelledOrRejectedBookings() {
        return cancelledOrRejectedBookings(null, null);
    }

    public long uniqueResourcesUsed() {
        return uniqueResourcesUsed(null, null);
    }

    public long nextSevenDaysApprovedBookings() {
        return nextSevenDaysApprovedBookings(null, null);
    }

    public long totalApprovedReservedMinutes() {
        return totalApprovedReservedMinutes(null, null);
    }

    private OffsetDateTime effectiveFrom(OffsetDateTime from) {
        return from == null ? MIN_WINDOW : from;
    }

    private OffsetDateTime effectiveTo(OffsetDateTime to) {
        return to == null ? MAX_WINDOW : to;
    }
}
