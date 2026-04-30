package com.teamresource.analytics.infra.persistence;

import com.teamresource.analytics.domain.BookingAnalyticsStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookingFactRepository extends JpaRepository<BookingFactEntity, UUID> {

    @Query("""
            select count(b) from BookingFactEntity b
            where b.startAt >= :from and b.startAt <= :to
            """)
    long countAll(OffsetDateTime from, OffsetDateTime to);

    @Query("""
            select count(b) from BookingFactEntity b
            where b.bookingStatus = :bookingStatus
            and b.startAt >= :from and b.startAt <= :to
            """)
    long countByBookingStatus(BookingAnalyticsStatus bookingStatus, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            select count(b) from BookingFactEntity b
            where b.bookingStatus in :statuses
            and b.startAt >= :from and b.startAt <= :to
            """)
    long countByBookingStatusIn(Collection<BookingAnalyticsStatus> statuses, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            select count(distinct b.resourceId) from BookingFactEntity b
            where b.bookingStatus in :statuses
            and b.startAt >= :from and b.startAt <= :to
            """)
    long countDistinctResourcesUsed(Collection<BookingAnalyticsStatus> statuses, OffsetDateTime from, OffsetDateTime to);

    @Query("select count(b) from BookingFactEntity b where b.bookingStatus = com.teamresource.analytics.domain.BookingAnalyticsStatus.APPROVED and b.startAt between :from and :to")
    long countApprovedBookingsBetween(OffsetDateTime from, OffsetDateTime to);

    @Query(value = """
            select coalesce(sum(extract(epoch from (end_at - start_at)) / 60), 0)
            from analytics.booking_facts
            where booking_status = 'APPROVED'
            and start_at >= :from
            and start_at <= :to
            """, nativeQuery = true)
    long sumApprovedReservedMinutes(OffsetDateTime from, OffsetDateTime to);

    @Query(value = """
            select
                linked_event_id as eventId,
                sum(case when booking_status in ('APPROVED', 'PENDING_APPROVAL') then 1 else 0 end) as activeBookings,
                sum(case when booking_status = 'WAITLISTED' then 1 else 0 end) as waitlistedBookings,
                sum(case when booking_status in ('CANCELLED', 'REJECTED') then 1 else 0 end) as cancelledBookings
            from analytics.booking_facts
            where linked_event_id is not null
            and start_at >= :from
            and start_at <= :to
            group by linked_event_id
            order by activeBookings desc, waitlistedBookings desc
            limit :limit
            """, nativeQuery = true)
    List<EventRegistrationProjection> aggregateEventRegistrations(OffsetDateTime from, OffsetDateTime to, int limit);

    @Query(value = """
            select
                resource_id as resourceId,
                count(*) as totalBookings,
                sum(case when booking_status = 'APPROVED' then 1 else 0 end) as approvedBookings,
                sum(case when booking_status = 'PENDING_APPROVAL' then 1 else 0 end) as pendingBookings,
                sum(case when booking_status in ('CANCELLED', 'REJECTED') then 1 else 0 end) as cancelledBookings,
                coalesce(sum(case when booking_status = 'APPROVED' then extract(epoch from (end_at - start_at)) / 60 else 0 end), 0) as bookedMinutes
            from analytics.booking_facts
            where start_at >= :from
            and start_at <= :to
            group by resource_id
            order by approvedBookings desc, bookedMinutes desc
            limit :limit
            """, nativeQuery = true)
    List<ResourceUsageProjection> aggregateResourceUsage(OffsetDateTime from, OffsetDateTime to, int limit);

    @Query(value = """
            select
                resource_id as resourceId,
                max(resource_name) as resourceName,
                max(resource_type) as resourceType,
                count(*) as totalBookings,
                sum(case when booking_status = 'APPROVED' then 1 else 0 end) as approvedBookings,
                sum(case when booking_status = 'PENDING_APPROVAL' then 1 else 0 end) as pendingBookings,
                sum(case when booking_status = 'WAITLISTED' then 1 else 0 end) as waitlistedBookings,
                sum(case when booking_status in ('CANCELLED', 'REJECTED') then 1 else 0 end) as cancelledBookings,
                coalesce(sum(case when booking_status = 'APPROVED' then extract(epoch from (end_at - start_at)) / 60 else 0 end), 0) as totalReservedMinutes
            from analytics.booking_facts
            group by resource_id
            order by approvedBookings desc, totalReservedMinutes desc
            """, nativeQuery = true)
    List<ResourcePopularityProjection> summarizeResourcePopularity();

    interface ResourcePopularityProjection {
        UUID getResourceId();
        String getResourceName();
        String getResourceType();
        long getTotalBookings();
        long getApprovedBookings();
        long getPendingBookings();
        long getWaitlistedBookings();
        long getCancelledBookings();
        long getTotalReservedMinutes();
    }

    interface EventRegistrationProjection {
        UUID getEventId();
        long getActiveBookings();
        long getWaitlistedBookings();
        long getCancelledBookings();
    }

    interface ResourceUsageProjection {
        UUID getResourceId();
        long getTotalBookings();
        long getApprovedBookings();
        long getPendingBookings();
        long getCancelledBookings();
        long getBookedMinutes();
    }
}
