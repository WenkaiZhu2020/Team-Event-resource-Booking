package com.teamresource.booking.infra.persistence;

import com.teamresource.booking.domain.BookingStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

    List<BookingEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<BookingEntity> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, BookingStatus status);

    List<BookingEntity> findByStatusOrderByCreatedAtAsc(BookingStatus status);

    List<BookingEntity> findByStatusAndResourceManagerIdOrderByCreatedAtAsc(BookingStatus status, UUID resourceManagerId);

    @Query("""
            select b from BookingEntity b
            where b.resourceId = :resourceId
              and b.status in :statuses
              and b.startAt < :endAt
              and b.endAt > :startAt
            order by b.startAt asc
            """)
    List<BookingEntity> findOverlappingBookings(
            UUID resourceId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            Collection<BookingStatus> statuses
    );

    @Query("""
            select b from BookingEntity b
            where b.resourceId = :resourceId
              and b.status = 'WAITLISTED'
            order by b.waitlistPosition asc nulls last, b.createdAt asc
            """)
    List<BookingEntity> findWaitlistedBookings(UUID resourceId);

    Optional<BookingEntity> findByResourceIdAndBookingId(UUID resourceId, UUID bookingId);
}
