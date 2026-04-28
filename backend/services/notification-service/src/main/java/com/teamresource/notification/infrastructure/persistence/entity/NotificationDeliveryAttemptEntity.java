package com.teamresource.notification.infrastructure.persistence.entity;

import com.teamresource.notification.domain.model.DeliveryAttemptStatus;
import com.teamresource.notification.domain.model.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_attempts", schema = "notifications")
public class NotificationDeliveryAttemptEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationChannel channel;

    @Column(nullable = false)
    private int attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DeliveryAttemptStatus status;

    @Column(length = 120)
    private String providerMessageId;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false)
    private OffsetDateTime attemptedAt;

    @Column(nullable = false)
    private long durationMs;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(int attemptNo) {
        this.attemptNo = attemptNo;
    }

    public DeliveryAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryAttemptStatus status) {
        this.status = status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(OffsetDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}
