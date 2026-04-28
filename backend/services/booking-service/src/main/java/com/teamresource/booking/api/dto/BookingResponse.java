package com.teamresource.booking.api.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.domain.model.BookingStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public final class BookingResponse {

    private final UUID id;
    private final UUID userId;
    private final UUID eventId;
    private final UUID resourceId;
    private final String resourceName;
    private final UUID resourceManagerId;
    private final String resourceType;
    private final OffsetDateTime startAt;
    private final OffsetDateTime endAt;
    private final String purpose;
    private final String status;
    private final String approvalMode;
    private final Integer waitlistPosition;
    private final OffsetDateTime approvalRequestedAt;
    private final OffsetDateTime decidedAt;
    private final String decisionNote;
    private final OffsetDateTime cancelledAt;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final ApprovalStatus approvalStatus;
    private final boolean approvalRequired;
    private final OffsetDateTime requestedAt;
    private final OffsetDateTime confirmedAt;
    private final String cancellationReason;
    private final OffsetDateTime rejectedAt;
    private final String rejectionReason;
    private final UUID approvedBy;
    private final OffsetDateTime approvedAt;
    private final String correlationId;
    private final WaitlistEntryResponse waitlist;

    public BookingResponse(
            UUID id,
            UUID userId,
            UUID eventId,
            UUID resourceId,
            String resourceName,
            UUID resourceManagerId,
            String resourceType,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String purpose,
            String status,
            String approvalMode,
            Integer waitlistPosition,
            OffsetDateTime approvalRequestedAt,
            OffsetDateTime decidedAt,
            String decisionNote,
            OffsetDateTime cancelledAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(
                id,
                userId,
                eventId,
                resourceId,
                resourceName,
                resourceManagerId,
                resourceType,
                startAt,
                endAt,
                purpose,
                normalizeStatus(status),
                approvalMode,
                waitlistPosition,
                approvalRequestedAt,
                decidedAt,
                decisionNote,
                cancelledAt,
                createdAt,
                updatedAt,
                deriveApprovalStatus(approvalMode, status),
                approvalMode != null && !"AUTO_APPROVE".equalsIgnoreCase(approvalMode),
                createdAt,
                "APPROVED".equalsIgnoreCase(status) ? decidedAt : null,
                null,
                "REJECTED".equalsIgnoreCase(status) ? decidedAt : null,
                "REJECTED".equalsIgnoreCase(status) ? decisionNote : null,
                null,
                null,
                null,
                waitlistPosition == null ? null : new WaitlistEntryResponse(null, waitlistPosition.longValue(), null, null)
        );
    }

    public BookingResponse(
            UUID id,
            UUID userId,
            UUID eventId,
            UUID resourceId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            BookingStatus status,
            ApprovalStatus approvalStatus,
            boolean approvalRequired,
            OffsetDateTime requestedAt,
            OffsetDateTime confirmedAt,
            OffsetDateTime cancelledAt,
            String cancellationReason,
            OffsetDateTime rejectedAt,
            String rejectionReason,
            UUID approvedBy,
            OffsetDateTime approvedAt,
            String correlationId,
            WaitlistEntryResponse waitlist
    ) {
        this(
                id,
                userId,
                eventId,
                resourceId,
                null,
                null,
                null,
                startAt,
                endAt,
                null,
                sourceStatusToApiStatus(status),
                approvalRequired ? "MANAGER_APPROVAL" : "AUTO_APPROVE",
                waitlist == null ? null : Math.toIntExact(waitlist.position()),
                approvalStatus == ApprovalStatus.PENDING ? requestedAt : null,
                approvedAt != null ? approvedAt : rejectedAt,
                rejectionReason,
                cancelledAt,
                requestedAt,
                approvedAt != null ? approvedAt : (rejectedAt != null ? rejectedAt : requestedAt),
                approvalStatus,
                approvalRequired,
                requestedAt,
                confirmedAt,
                cancellationReason,
                rejectedAt,
                rejectionReason,
                approvedBy,
                approvedAt,
                correlationId,
                waitlist
        );
    }

    public BookingResponse(
            UUID id,
            UUID userId,
            UUID eventId,
            UUID resourceId,
            String resourceName,
            UUID resourceManagerId,
            String resourceType,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String purpose,
            String status,
            String approvalMode,
            Integer waitlistPosition,
            OffsetDateTime approvalRequestedAt,
            OffsetDateTime decidedAt,
            String decisionNote,
            OffsetDateTime cancelledAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            ApprovalStatus approvalStatus,
            boolean approvalRequired,
            OffsetDateTime requestedAt,
            OffsetDateTime confirmedAt,
            String cancellationReason,
            OffsetDateTime rejectedAt,
            String rejectionReason,
            UUID approvedBy,
            OffsetDateTime approvedAt,
            String correlationId,
            WaitlistEntryResponse waitlist
    ) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceManagerId = resourceManagerId;
        this.resourceType = resourceType;
        this.startAt = startAt;
        this.endAt = endAt;
        this.purpose = purpose;
        this.status = normalizeStatus(status);
        this.approvalMode = approvalMode;
        this.waitlistPosition = waitlistPosition;
        this.approvalRequestedAt = approvalRequestedAt;
        this.decidedAt = decidedAt;
        this.decisionNote = decisionNote;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.approvalStatus = approvalStatus;
        this.approvalRequired = approvalRequired;
        this.requestedAt = requestedAt;
        this.confirmedAt = confirmedAt;
        this.cancellationReason = cancellationReason;
        this.rejectedAt = rejectedAt;
        this.rejectionReason = rejectionReason;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.correlationId = correlationId;
        this.waitlist = waitlist;
    }

    private static String sourceStatusToApiStatus(BookingStatus status) {
        if (status == null) {
            return null;
        }
        return status == BookingStatus.APPROVED ? "APPROVED" : status.name();
    }

    private static String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        return "CONFIRMED".equalsIgnoreCase(status) ? "APPROVED" : status.toUpperCase();
    }

    private static ApprovalStatus deriveApprovalStatus(String approvalMode, String status) {
        if (approvalMode == null) {
            return null;
        }
        if ("AUTO_APPROVE".equalsIgnoreCase(approvalMode)) {
            return ApprovalStatus.NOT_REQUIRED;
        }
        if ("APPROVED".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status)) {
            return ApprovalStatus.APPROVED;
        }
        if ("REJECTED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
            return ApprovalStatus.REJECTED;
        }
        return ApprovalStatus.PENDING;
    }

    public UUID id() { return id; }
    @JsonIgnore
    public UUID bookingId() { return id; }
    public UUID userId() { return userId; }
    public UUID eventId() { return eventId; }
    @JsonIgnore
    public UUID linkedEventId() { return eventId; }
    public UUID resourceId() { return resourceId; }
    public String resourceName() { return resourceName; }
    public UUID resourceManagerId() { return resourceManagerId; }
    public String resourceType() { return resourceType; }
    public OffsetDateTime startAt() { return startAt; }
    public OffsetDateTime endAt() { return endAt; }
    public String purpose() { return purpose; }
    public String status() { return status; }
    @JsonIgnore
    public BookingStatus statusEnum() {
        if (status == null) {
            return null;
        }
        return "APPROVED".equals(status) ? BookingStatus.APPROVED : BookingStatus.valueOf(status);
    }
    public String approvalMode() { return approvalMode; }
    public Integer waitlistPosition() { return waitlistPosition; }
    public OffsetDateTime approvalRequestedAt() { return approvalRequestedAt; }
    public OffsetDateTime decidedAt() { return decidedAt; }
    public String decisionNote() { return decisionNote; }
    public OffsetDateTime cancelledAt() { return cancelledAt; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime updatedAt() { return updatedAt; }
    public ApprovalStatus approvalStatus() { return approvalStatus; }
    public boolean approvalRequired() { return approvalRequired; }
    public OffsetDateTime requestedAt() { return requestedAt; }
    public OffsetDateTime confirmedAt() { return confirmedAt; }
    public String cancellationReason() { return cancellationReason; }
    public OffsetDateTime rejectedAt() { return rejectedAt; }
    public String rejectionReason() { return rejectionReason; }
    public UUID approvedBy() { return approvedBy; }
    public OffsetDateTime approvedAt() { return approvedAt; }
    public String correlationId() { return correlationId; }
    public WaitlistEntryResponse waitlist() { return waitlist; }
}
