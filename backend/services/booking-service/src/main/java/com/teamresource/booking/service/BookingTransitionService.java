package com.teamresource.booking.service;

import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.domain.BookingStatus;
import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.infra.persistence.BookingEntity;
import com.teamresource.booking.infra.persistence.BookingRepository;
import com.teamresource.booking.lock.ResourceLockService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service("bookingApiTransitionService")
public class BookingTransitionService {

    private final BookingRepository bookingRepository;
    private final ResourceLockService resourceLockService;
    private final BookingOutboxService bookingOutboxService;

    public BookingTransitionService(
            BookingRepository bookingRepository,
            ResourceLockService resourceLockService,
            BookingOutboxService bookingOutboxService
    ) {
        this.bookingRepository = bookingRepository;
        this.resourceLockService = resourceLockService;
        this.bookingOutboxService = bookingOutboxService;
    }

    public BookingEntity cancel(BookingEntity entity) {
        if (entity.getStatus() == BookingStatus.CANCELLED || entity.getStatus() == BookingStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is already closed");
        }

        return resourceLockService.executeWithResourceLock(entity.getResourceId(), () -> {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            entity.setStatus(BookingStatus.CANCELLED);
            entity.setCancelledAt(now);
            entity.setCancellationReason(noteOrDefault(entity.getDecisionNote()));
            entity.setApprovalStatus(entity.getApprovalStatus() == null ? ApprovalStatus.REJECTED : entity.getApprovalStatus());
            entity.setUpdatedAt(now);
            BookingEntity saved = bookingRepository.save(entity);
            bookingOutboxService.record("booking.cancelled", toResponse(saved));
            return saved;
        });
    }

    public BookingEntity approve(BookingEntity entity, String note, Set<BookingStatus> occupyingStatuses) {
        if (entity.getStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending bookings can be approved");
        }

        return resourceLockService.executeWithResourceLock(entity.getResourceId(), () -> {
            ensureNoConflictsExcluding(entity, occupyingStatuses);
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            entity.setStatus(BookingStatus.APPROVED);
            entity.setDecidedAt(now);
            entity.setDecisionNote(note);
            entity.setApprovalStatus(ApprovalStatus.APPROVED);
            entity.setConfirmedAt(now);
            entity.setApprovedAt(now);
            entity.setUpdatedAt(now);
            BookingEntity saved = bookingRepository.save(entity);
            bookingOutboxService.record("booking.approved", toResponse(saved));
            return saved;
        });
    }

    public BookingEntity reject(BookingEntity entity, String note) {
        if (entity.getStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending bookings can be rejected");
        }

        return resourceLockService.executeWithResourceLock(entity.getResourceId(), () -> {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            entity.setStatus(BookingStatus.REJECTED);
            entity.setDecidedAt(now);
            entity.setDecisionNote(note);
            entity.setApprovalStatus(ApprovalStatus.REJECTED);
            entity.setRejectedAt(now);
            entity.setRejectionReason(note);
            entity.setUpdatedAt(now);
            BookingEntity saved = bookingRepository.save(entity);
            bookingOutboxService.record("booking.rejected", toResponse(saved));
            return saved;
        });
    }

    private void ensureNoConflictsExcluding(BookingEntity entity, Set<BookingStatus> occupyingStatuses) {
        boolean conflict = bookingRepository.findOverlappingBookings(
                        entity.getResourceId(),
                        entity.getStartAt(),
                        entity.getEndAt(),
                        occupyingStatuses)
                .stream()
                .anyMatch(other -> !other.getBookingId().equals(entity.getBookingId()));
        if (conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking can no longer be approved because the slot is occupied");
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
                                com.teamresource.booking.domain.model.WaitlistStatus.WAITING,
                                null
                        )
        );
    }

    private String noteOrDefault(String note) {
        return note == null || note.isBlank() ? "Booking cancelled" : note;
    }
}
