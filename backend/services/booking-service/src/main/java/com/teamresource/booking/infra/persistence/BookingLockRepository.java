package com.teamresource.booking.infra.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface BookingLockRepository extends JpaRepository<BookingLockEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from BookingLockEntity l where l.resourceId = :resourceId")
    Optional<BookingLockEntity> lockByResourceId(UUID resourceId);
}
