package com.teamresource.booking.infra.persistence;

import com.teamresource.booking.domain.ApprovalMode;
import com.teamresource.booking.domain.BookingStatus;
import com.teamresource.booking.domain.model.ApprovalStatus;
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
@Table(name = "bookings", schema = "bookings")
public class BookingEntity {

    @Id
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "linked_event_id")
    private UUID linkedEventId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "resource_name", nullable = false, length = 160)
    private String resourceName;

    @Column(name = "resource_manager_id", nullable = false)
    private UUID resourceManagerId;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Column(nullable = false, length = 240)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_mode", nullable = false, length = 32)
    private ApprovalMode approvalMode;

    @Column(name = "waitlist_position")
    private Integer waitlistPosition;

    @Column(name = "approval_requested_at")
    private OffsetDateTime approvalRequestedAt;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "decision_note", length = 400)
    private String decisionNote;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 40)
    private ApprovalStatus approvalStatus;

    @Column(name = "requested_at")
    private OffsetDateTime requestedAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 300)
    private String rejectionReason;

    @Column(name = "approval_required")
    private Boolean approvalRequired;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private Long version;

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getLinkedEventId() {
        return linkedEventId;
    }

    public void setLinkedEventId(UUID linkedEventId) {
        this.linkedEventId = linkedEventId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public UUID getResourceManagerId() {
        return resourceManagerId;
    }

    public void setResourceManagerId(UUID resourceManagerId) {
        this.resourceManagerId = resourceManagerId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public ApprovalMode getApprovalMode() {
        return approvalMode;
    }

    public void setApprovalMode(ApprovalMode approvalMode) {
        this.approvalMode = approvalMode;
    }

    public Integer getWaitlistPosition() {
        return waitlistPosition;
    }

    public void setWaitlistPosition(Integer waitlistPosition) {
        this.waitlistPosition = waitlistPosition;
    }

    public OffsetDateTime getApprovalRequestedAt() {
        return approvalRequestedAt;
    }

    public void setApprovalRequestedAt(OffsetDateTime approvalRequestedAt) {
        this.approvalRequestedAt = approvalRequestedAt;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(OffsetDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(OffsetDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(OffsetDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public OffsetDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(OffsetDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public OffsetDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(OffsetDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Boolean getApprovalRequired() {
        return approvalRequired;
    }

    public void setApprovalRequired(Boolean approvalRequired) {
        this.approvalRequired = approvalRequired;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
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
}
