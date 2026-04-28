package com.teamresource.booking.infrastructure.persistence.repository;

import com.teamresource.booking.domain.model.WaitlistStatus;
import com.teamresource.booking.domain.repository.WaitlistRepository;
import com.teamresource.booking.infrastructure.persistence.entity.WaitlistEntryEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class WaitlistRepositoryImpl implements WaitlistRepository {

    private final SpringDataWaitlistJpaRepository repository;

    public WaitlistRepositoryImpl(SpringDataWaitlistJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public WaitlistEntryEntity save(WaitlistEntryEntity entity) {
        return repository.save(entity);
    }

    @Override
    public Optional<WaitlistEntryEntity> findByBookingId(UUID bookingId) {
        return repository.findByBookingId(bookingId);
    }

    @Override
    public long nextPosition(UUID resourceId, OffsetDateTime startAt, OffsetDateTime endAt) {
        return repository.maxPosition(resourceId, startAt, endAt) + 1;
    }

    @Override
    public Optional<WaitlistEntryEntity> findNextWaiting(UUID resourceId, OffsetDateTime startAt, OffsetDateTime endAt) {
        return repository.findWaitingOrdered(resourceId, startAt, endAt).stream().findFirst();
    }

    @Override
    public List<WaitlistEntryEntity> findByResourceId(UUID resourceId, WaitlistStatus status) {
        return repository.findByResourceIdAndStatusOrderByPositionIndexAsc(resourceId, status);
    }
}
