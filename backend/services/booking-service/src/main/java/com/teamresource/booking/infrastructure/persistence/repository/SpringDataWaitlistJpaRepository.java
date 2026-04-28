package com.teamresource.booking.infrastructure.persistence.repository;

import com.teamresource.booking.domain.model.WaitlistStatus;
import com.teamresource.booking.infrastructure.persistence.entity.WaitlistEntryEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataWaitlistJpaRepository extends JpaRepository<WaitlistEntryEntity, UUID> {

    Optional<WaitlistEntryEntity> findByBookingId(UUID bookingId);

    @Query("""
            select coalesce(max(w.positionIndex), 0)
            from WaitlistEntryEntity w
            where w.resourceId = :resourceId
              and w.startAt = :startAt
              and w.endAt = :endAt
            """)
    long maxPosition(
            @Param("resourceId") UUID resourceId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

    @Query("""
            select w
            from WaitlistEntryEntity w
            where w.resourceId = :resourceId
              and w.startAt = :startAt
              and w.endAt = :endAt
              and w.status = com.teamresource.booking.domain.model.WaitlistStatus.WAITING
            order by w.positionIndex asc
            """)
    List<WaitlistEntryEntity> findWaitingOrdered(
            @Param("resourceId") UUID resourceId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

    List<WaitlistEntryEntity> findByResourceIdAndStatusOrderByPositionIndexAsc(UUID resourceId, WaitlistStatus status);
}
