package com.teamresource.booking.infrastructure.persistence.entity;

import com.teamresource.booking.domain.model.WaitlistStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "waitlist_entries", schema = "bookings")
public class WaitlistEntryEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID bookingId;

    @Column(nullable = false)
    private UUID resourceId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private OffsetDateTime startAt;

    @Column(nullable = false)
    private OffsetDateTime endAt;

    @Column(nullable = false)
    private Long positionIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WaitlistStatus status;

    private OffsetDateTime promotedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private Long version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(OffsetDateTime startAt) {
        this.startAt = startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(OffsetDateTime endAt) {
        this.endAt = endAt;
    }

    public Long getPositionIndex() {
        return positionIndex;
    }

    public void setPositionIndex(Long positionIndex) {
        this.positionIndex = positionIndex;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public void setStatus(WaitlistStatus status) {
        this.status = status;
    }

    public OffsetDateTime getPromotedAt() {
        return promotedAt;
    }

    public void setPromotedAt(OffsetDateTime promotedAt) {
        this.promotedAt = promotedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
