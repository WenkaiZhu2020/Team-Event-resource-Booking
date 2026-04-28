package com.teamresource.booking.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "resource_booking_locks", schema = "bookings")
public class ResourceBookingLockEntity {

    @Id
    private UUID resourceId;

    @Version
    private Long version;

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
