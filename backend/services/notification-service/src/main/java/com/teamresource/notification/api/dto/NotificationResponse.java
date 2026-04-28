package com.teamresource.notification.api.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public final class NotificationResponse {

    private final UUID id;
    private final UUID userId;
    private final UUID sourceEventId;
    private final String sourceEventType;
    private final String notificationType;
    private final String channel;
    private final String title;
    private final String body;
    private final String status;
    private final OffsetDateTime readAt;
    private final OffsetDateTime sentAt;
    private final String failureReason;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final String source;
    private final String referenceType;
    private final UUID referenceId;
    private final OffsetDateTime scheduledAt;

    public NotificationResponse(
            UUID notificationId,
            UUID userId,
            UUID sourceEventId,
            String sourceEventType,
            String notificationType,
            String channel,
            String subject,
            String body,
            String status,
            OffsetDateTime readAt,
            OffsetDateTime sentAt,
            String failureReason,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(notificationId, userId, sourceEventId, sourceEventType, notificationType, channel, subject, body, status,
                readAt, sentAt, failureReason, createdAt, updatedAt, null, null, null, null);
    }

    public NotificationResponse(
            UUID id,
            UUID userId,
            com.teamresource.notification.domain.model.NotificationType type,
            com.teamresource.notification.domain.model.NotificationChannel channel,
            com.teamresource.notification.domain.model.NotificationStatus status,
            String title,
            String body,
            String source,
            String sourceEventType,
            String referenceType,
            UUID referenceId,
            OffsetDateTime scheduledAt,
            OffsetDateTime sentAt,
            OffsetDateTime readAt,
            OffsetDateTime createdAt
    ) {
        this(id, userId, null, sourceEventType, type.name(), channel.name(), title, body, status.name(), readAt, sentAt, null,
                createdAt, createdAt, source, referenceType, referenceId, scheduledAt);
    }

    public NotificationResponse(
            UUID id,
            UUID userId,
            UUID sourceEventId,
            String sourceEventType,
            String notificationType,
            String channel,
            String title,
            String body,
            String status,
            OffsetDateTime readAt,
            OffsetDateTime sentAt,
            String failureReason,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String source,
            String referenceType,
            UUID referenceId,
            OffsetDateTime scheduledAt
    ) {
        this.id = id;
        this.userId = userId;
        this.sourceEventId = sourceEventId;
        this.sourceEventType = sourceEventType;
        this.notificationType = notificationType;
        this.channel = channel;
        this.title = title;
        this.body = body;
        this.status = status;
        this.readAt = readAt;
        this.sentAt = sentAt;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.source = source;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.scheduledAt = scheduledAt;
    }

    public UUID id() { return id; }
    @JsonIgnore
    public UUID notificationId() { return id; }
    public UUID userId() { return userId; }
    public UUID sourceEventId() { return sourceEventId; }
    public String sourceEventType() { return sourceEventType; }
    public String notificationType() { return notificationType; }
    @JsonIgnore
    public String type() { return notificationType; }
    public String channel() { return channel; }
    public String title() { return title; }
    @JsonProperty("subject")
    public String subject() { return title; }
    public String body() { return body; }
    public String status() { return status; }
    public OffsetDateTime readAt() { return readAt; }
    public OffsetDateTime sentAt() { return sentAt; }
    public String failureReason() { return failureReason; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime updatedAt() { return updatedAt; }
    public String source() { return source; }
    public String referenceType() { return referenceType; }
    public UUID referenceId() { return referenceId; }
    public OffsetDateTime scheduledAt() { return scheduledAt; }
}
