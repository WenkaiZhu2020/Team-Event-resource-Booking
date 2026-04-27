package com.teamresource.notification.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events", schema = "notifications")
public class ProcessedEventEntity {

    @Id
    @Column(name = "processed_event_id", nullable = false)
    private UUID processedEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    public UUID getProcessedEventId() { return processedEventId; }
    public void setProcessedEventId(UUID processedEventId) { this.processedEventId = processedEventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public UUID getAggregateId() { return aggregateId; }
    public void setAggregateId(UUID aggregateId) { this.aggregateId = aggregateId; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}
