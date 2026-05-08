package com.teamresource.notification.infra.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<IdempotencyRecordEntity> findByAggregateId(UUID aggregateId);
}
