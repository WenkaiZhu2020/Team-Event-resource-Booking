package com.teamresource.booking.infrastructure.persistence.repository;

import com.teamresource.booking.infrastructure.persistence.entity.ResourceBookingLockEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataResourceBookingLockJpaRepository extends JpaRepository<ResourceBookingLockEntity, UUID> {

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from ResourceBookingLockEntity l where l.resourceId = :resourceId")
    ResourceBookingLockEntity lockByResourceId(@Param("resourceId") UUID resourceId);
}
