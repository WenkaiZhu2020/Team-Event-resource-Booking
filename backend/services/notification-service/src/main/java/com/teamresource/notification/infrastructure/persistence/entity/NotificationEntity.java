package com.teamresource.notification.infrastructure.persistence.entity;

import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.domain.model.NotificationStatus;
import com.teamresource.notification.domain.model.NotificationType;
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
@Table(name = "notifications", schema = "notifications")
public class NotificationEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationStatus status;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(length = 80)
    private String templateCode;

    @Column(nullable = false, length = 60)
    private String source;

    @Column(length = 120)
    private String sourceEventType;

    @Column(length = 64)
    private String referenceType;

    private UUID referenceId;

    @Column(nullable = false, length = 180)
    private String idempotencyKey;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    private OffsetDateTime scheduledAt;

    private OffsetDateTime sentAt;

    private OffsetDateTime readAt;

    private OffsetDateTime nextRetryAt;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private int maxRetries;

    @Column(length = 500)
    private String lastError;

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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceEventType() {
        return sourceEventType;
    }

    public void setSourceEventType(String sourceEventType) {
        this.sourceEventType = sourceEventType;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(OffsetDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(OffsetDateTime readAt) {
        this.readAt = readAt;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(OffsetDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
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
