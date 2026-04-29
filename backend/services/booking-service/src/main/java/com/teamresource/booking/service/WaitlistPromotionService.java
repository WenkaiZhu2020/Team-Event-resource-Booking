package com.teamresource.booking.service;

import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.domain.ApprovalMode;
import com.teamresource.booking.domain.BookingStatus;
import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.infra.client.WorkflowClient;
import com.teamresource.booking.infra.persistence.BookingEntity;
import com.teamresource.booking.infra.persistence.BookingRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class WaitlistPromotionService {

    private final BookingRepository bookingRepository;
    private final WorkflowClient workflowClient;
    private final BookingOutboxService bookingOutboxService;

    public WaitlistPromotionService(
            BookingRepository bookingRepository,
            WorkflowClient workflowClient,
            BookingOutboxService bookingOutboxService
    ) {
        this.bookingRepository = bookingRepository;
        this.workflowClient = workflowClient;
        this.bookingOutboxService = bookingOutboxService;
    }

    public void promote(UUID resourceId, Set<BookingStatus> occupyingStatuses) {
        List<BookingEntity> candidates = bookingRepository.findWaitlistedBookings(resourceId);
        for (BookingEntity candidate : candidates) {
            boolean stillBlocked = bookingRepository.findOverlappingBookings(
                            candidate.getResourceId(),
                            candidate.getStartAt(),
                            candidate.getEndAt(),
                            occupyingStatuses)
                    .stream()
                    .anyMatch(other -> !other.getBookingId().equals(candidate.getBookingId()));
            if (stillBlocked) {
                continue;
            }
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            candidate.setWaitlistPosition(null);
            if (candidate.getApprovalMode() == ApprovalMode.AUTO_APPROVE) {
                candidate.setStatus(BookingStatus.APPROVED);
                candidate.setApprovalStatus(ApprovalStatus.NOT_REQUIRED);
                candidate.setConfirmedAt(now);
                candidate.setApprovedAt(now);
            } else {
                candidate.setStatus(BookingStatus.PENDING_APPROVAL);
                candidate.setApprovalRequestedAt(now);
                candidate.setApprovalStatus(ApprovalStatus.PENDING);
            }
            candidate.setUpdatedAt(now);
            BookingEntity saved = bookingRepository.save(candidate);
            BookingResponse response = toResponse(saved);
            if (saved.getStatus() == BookingStatus.PENDING_APPROVAL) {
                workflowClient.createBookingApproval(response);
            }
            bookingOutboxService.record("booking.waitlist.promoted", response);
        }
    }

    private BookingResponse toResponse(BookingEntity entity) {
        return new BookingResponse(
                entity.getBookingId(),
                entity.getUserId(),
                entity.getLinkedEventId(),
                entity.getResourceId(),
                entity.getResourceName(),
                entity.getResourceManagerId(),
                entity.getResourceType(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getPurpose(),
                entity.getStatus().name(),
                entity.getApprovalMode().name(),
                entity.getWaitlistPosition(),
                entity.getApprovalRequestedAt(),
                entity.getDecidedAt(),
                entity.getDecisionNote(),
                entity.getCancelledAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getApprovalStatus(),
                Boolean.TRUE.equals(entity.getApprovalRequired()),
                entity.getRequestedAt(),
                entity.getConfirmedAt(),
                entity.getCancellationReason(),
                entity.getRejectedAt(),
                entity.getRejectionReason(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getCorrelationId(),
                entity.getWaitlistPosition() == null
                        ? null
                        : new com.teamresource.booking.api.dto.WaitlistEntryResponse(
                                entity.getBookingId(),
                                entity.getWaitlistPosition().longValue(),
                                com.teamresource.booking.domain.model.WaitlistStatus.PROMOTED,
                                entity.getApprovedAt() != null ? entity.getApprovedAt() : entity.getApprovalRequestedAt()
                        )
        );
    }

}
