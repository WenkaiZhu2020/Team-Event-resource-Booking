package com.teamresource.notification.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "consumed_messages", schema = "notifications")
public class ConsumedMessageEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(nullable = false, length = 150)
    private String messageId;

    @Column(length = 128)
    private String payloadHash;

    @Column(nullable = false)
    private OffsetDateTime consumedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(OffsetDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }
}
