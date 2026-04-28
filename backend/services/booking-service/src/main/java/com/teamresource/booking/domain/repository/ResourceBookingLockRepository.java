package com.teamresource.booking.domain.repository;

import com.teamresource.booking.infrastructure.persistence.entity.ResourceBookingLockEntity;
import java.util.UUID;

public interface ResourceBookingLockRepository {

    ResourceBookingLockEntity acquireLock(UUID resourceId);
}
