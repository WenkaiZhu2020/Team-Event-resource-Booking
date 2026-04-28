package com.teamresource.booking.infrastructure.persistence.repository;

import com.teamresource.booking.domain.model.BookingStatus;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataBookingJpaRepository extends JpaRepository<BookingEntity, UUID>, JpaSpecificationExecutor<BookingEntity> {

    @Query("""
            select count(b) > 0
            from BookingEntity b
            where b.resourceId = :resourceId
              and b.status in :statuses
              and b.startAt < :endAt
              and b.endAt > :startAt
            """)
    boolean existsOverlap(
            @Param("resourceId") UUID resourceId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
            select b
            from BookingEntity b
            where b.eventId = :eventId
              and b.startAt >= :now
              and b.status in (
                com.teamresource.booking.domain.model.BookingStatus.PENDING_APPROVAL,
                com.teamresource.booking.domain.model.BookingStatus.APPROVED,
                com.teamresource.booking.domain.model.BookingStatus.WAITLISTED
              )
            """)
    List<BookingEntity> findFutureByEvent(@Param("eventId") UUID eventId, @Param("now") OffsetDateTime now);

    @Query("""
            select b
            from BookingEntity b
            where b.resourceId = :resourceId
              and b.startAt >= :now
              and b.status in (
                com.teamresource.booking.domain.model.BookingStatus.PENDING_APPROVAL,
                com.teamresource.booking.domain.model.BookingStatus.APPROVED,
                com.teamresource.booking.domain.model.BookingStatus.WAITLISTED
              )
            """)
    List<BookingEntity> findFutureByResource(@Param("resourceId") UUID resourceId, @Param("now") OffsetDateTime now);
}
