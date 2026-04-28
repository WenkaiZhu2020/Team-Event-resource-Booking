package com.teamresource.booking.domain.repository;

import com.teamresource.booking.domain.model.WaitlistStatus;
import com.teamresource.booking.infrastructure.persistence.entity.WaitlistEntryEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaitlistRepository {

    WaitlistEntryEntity save(WaitlistEntryEntity entity);

    Optional<WaitlistEntryEntity> findByBookingId(UUID bookingId);

    long nextPosition(UUID resourceId, OffsetDateTime startAt, OffsetDateTime endAt);

    Optional<WaitlistEntryEntity> findNextWaiting(UUID resourceId, OffsetDateTime startAt, OffsetDateTime endAt);

    List<WaitlistEntryEntity> findByResourceId(UUID resourceId, WaitlistStatus status);
}
