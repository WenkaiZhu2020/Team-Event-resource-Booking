package com.teamresource.notification.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records", schema = "notifications")
public class IdempotencyRecordEntity {

    @Id
    @Column(name = "idempotency_record_id", nullable = false)
    private UUID idempotencyRecordId;

    @Column(name = "aggregate_id", nullable = false, unique = true)
    private UUID aggregateId;

    @Column(name = "last_processed_message_id", nullable = false)
    private UUID lastProcessedMessageId;

    @Column(name = "last_processed_event_type", nullable = false, length = 120)
    private String lastProcessedEventType;

    @Column(name = "last_processed_event_at", nullable = false)
    private OffsetDateTime lastProcessedEventAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getIdempotencyRecordId() {
        return idempotencyRecordId;
    }

    public void setIdempotencyRecordId(UUID idempotencyRecordId) {
        this.idempotencyRecordId = idempotencyRecordId;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(UUID aggregateId) {
        this.aggregateId = aggregateId;
    }

    public UUID getLastProcessedMessageId() {
        return lastProcessedMessageId;
    }

    public void setLastProcessedMessageId(UUID lastProcessedMessageId) {
        this.lastProcessedMessageId = lastProcessedMessageId;
    }

    public String getLastProcessedEventType() {
        return lastProcessedEventType;
    }

    public void setLastProcessedEventType(String lastProcessedEventType) {
        this.lastProcessedEventType = lastProcessedEventType;
    }

    public OffsetDateTime getLastProcessedEventAt() {
        return lastProcessedEventAt;
    }

    public void setLastProcessedEventAt(OffsetDateTime lastProcessedEventAt) {
        this.lastProcessedEventAt = lastProcessedEventAt;
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
}
