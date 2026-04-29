package com.teamresource.booking.service;

import com.teamresource.booking.api.dto.BookingDecisionRequest;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.api.dto.CreateBookingRequest;
import com.teamresource.booking.domain.ApprovalMode;
import com.teamresource.booking.domain.BookingStatus;
import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.infra.client.EventClient;
import com.teamresource.booking.infra.client.ResourceClient;
import com.teamresource.booking.infra.client.WorkflowClient;
import com.teamresource.booking.infra.persistence.BookingEntity;
import com.teamresource.booking.infra.persistence.BookingLockRepository;
import com.teamresource.booking.infra.persistence.BookingRepository;
import com.teamresource.booking.infra.persistence.IdempotencyRecordEntity;
import com.teamresource.booking.infra.persistence.IdempotencyRecordRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {

    private static final Set<BookingStatus> OCCUPYING_STATUSES = EnumSet.of(BookingStatus.APPROVED, BookingStatus.PENDING_APPROVAL);

    private final BookingRepository bookingRepository;
    private final BookingLockRepository bookingLockRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ResourceClient resourceClient;
    private final EventClient eventClient;
    private final WorkflowClient workflowClient;
    private final BookingOutboxService bookingOutboxService;
    private final BookingTransitionService bookingTransitionService;
    private final WaitlistPromotionService waitlistPromotionService;
    private final BookingLockBootstrapService bookingLockBootstrapService;

    public BookingService(
            BookingRepository bookingRepository,
            BookingLockRepository bookingLockRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            ResourceClient resourceClient,
            EventClient eventClient,
            WorkflowClient workflowClient,
            BookingOutboxService bookingOutboxService,
            BookingTransitionService bookingTransitionService,
            WaitlistPromotionService waitlistPromotionService,
            BookingLockBootstrapService bookingLockBootstrapService
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingLockRepository = bookingLockRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.resourceClient = resourceClient;
        this.eventClient = eventClient;
        this.workflowClient = workflowClient;
        this.bookingOutboxService = bookingOutboxService;
        this.bookingTransitionService = bookingTransitionService;
        this.waitlistPromotionService = waitlistPromotionService;
        this.bookingLockBootstrapService = bookingLockBootstrapService;
    }

    @Transactional
    public BookingResponse create(UUID userId, CreateBookingRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyRecordRepository.findByIdempotencyKeyAndUserId(idempotencyKey.trim(), userId);
            if (existing.isPresent()) {
                return toResponse(findBooking(existing.get().getBookingId()));
            }
        }

        validateWindow(request.startAt(), request.endAt());
        ResourceClient.ResourceSnapshot resource = resourceClient.getResource(request.resourceId());
        ensureBookableResource(resource, request.startAt(), request.endAt());
        if (request.linkedEventId() != null) {
            ensureLinkableEvent(request.linkedEventId());
        }

        acquireResourceLock(resource.resourceId());

        List<BookingEntity> overlapping = bookingRepository.findOverlappingBookings(
                resource.resourceId(),
                request.startAt(),
                request.endAt(),
                OCCUPYING_STATUSES
        );

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        BookingEntity entity = new BookingEntity();
        entity.setBookingId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setLinkedEventId(request.linkedEventId());
        entity.setResourceId(resource.resourceId());
        entity.setResourceName(resource.name());
        entity.setResourceManagerId(resource.managerId());
        entity.setResourceType(resource.type());
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setPurpose(request.purpose().trim());
        entity.setApprovalMode(parseApprovalMode(resource.approvalMode()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setRequestedAt(now);
        entity.setApprovalRequired(entity.getApprovalMode() != ApprovalMode.AUTO_APPROVE);
        entity.setCorrelationId(trimToNull(idempotencyKey));

        if (!overlapping.isEmpty()) {
            if (!resource.allowWaitlist()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The requested time slot is already occupied");
            }
            entity.setStatus(BookingStatus.WAITLISTED);
            entity.setWaitlistPosition(nextWaitlistPosition(resource.resourceId()));
            entity.setApprovalStatus(entity.getApprovalRequired() ? ApprovalStatus.PENDING : ApprovalStatus.NOT_REQUIRED);
        } else if (entity.getApprovalMode() == ApprovalMode.AUTO_APPROVE) {
            entity.setStatus(BookingStatus.APPROVED);
            entity.setApprovalStatus(ApprovalStatus.NOT_REQUIRED);
            entity.setConfirmedAt(now);
            entity.setApprovedAt(now);
        } else {
            entity.setStatus(BookingStatus.PENDING_APPROVAL);
            entity.setApprovalRequestedAt(now);
            entity.setApprovalStatus(ApprovalStatus.PENDING);
        }

        BookingEntity saved = bookingRepository.save(entity);
        storeIdempotencyRecord(saved, userId, idempotencyKey, now);
        BookingResponse response = toResponse(saved);
        if (saved.getStatus() == BookingStatus.PENDING_APPROVAL) {
            workflowClient.createBookingApproval(response);
        }
        bookingOutboxService.record("booking.created", response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(UUID userId, String status) {
        if (status == null || status.isBlank()) {
            return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
        }
        return bookingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, parseStatus(status)).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse byId(UUID bookingId, UUID currentUserId, boolean admin) {
        BookingEntity entity = findBooking(bookingId);
        requireOwnerManagerOrAdmin(entity, currentUserId, admin);
        return toResponse(entity);
    }

    @Transactional
    public BookingResponse cancel(UUID bookingId, UUID currentUserId, boolean admin) {
        BookingEntity entity = findBooking(bookingId);
        if (!admin && !entity.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Booking access denied");
        }
        if (entity.getStatus() == BookingStatus.CANCELLED || entity.getStatus() == BookingStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is already closed");
        }

        BookingEntity saved = bookingTransitionService.cancel(entity);
        BookingResponse response = toResponse(saved);
        waitlistPromotionService.promote(saved.getResourceId(), OCCUPYING_STATUSES);
        return response;
    }


    @Transactional(readOnly = true)
    public Page<BookingResponse> search(
            UUID userId,
            UUID resourceId,
            String status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    ) {
        List<BookingResponse> filtered = bookingRepository.findAll().stream()
                .filter(entity -> userId == null || userId.equals(entity.getUserId()))
                .filter(entity -> resourceId == null || resourceId.equals(entity.getResourceId()))
                .filter(entity -> status == null || status.isBlank() || entity.getStatus() == parseStatus(status))
                .filter(entity -> from == null || !entity.getEndAt().isBefore(from))
                .filter(entity -> to == null || !entity.getStartAt().isAfter(to))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .toList();

        int offset = (int) pageable.getOffset();
        int end = Math.min(offset + pageable.getPageSize(), filtered.size());
        List<BookingResponse> pageContent = offset >= filtered.size() ? List.of() : filtered.subList(offset, end);
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public List<com.teamresource.booking.api.dto.WaitlistEntryResponse> listWaitlist(UUID resourceId) {
        return bookingRepository.findWaitlistedBookings(resourceId).stream()
                .map(entity -> new com.teamresource.booking.api.dto.WaitlistEntryResponse(
                        entity.getBookingId(),
                        entity.getWaitlistPosition() == null ? null : entity.getWaitlistPosition().longValue(),
                        com.teamresource.booking.domain.model.WaitlistStatus.WAITING,
                        null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> pendingApprovals(UUID currentUserId, boolean admin) {
        List<BookingEntity> entities = admin
                ? bookingRepository.findByStatusOrderByCreatedAtAsc(BookingStatus.PENDING_APPROVAL)
                : bookingRepository.findByStatusAndResourceManagerIdOrderByCreatedAtAsc(BookingStatus.PENDING_APPROVAL, currentUserId);
        return entities.stream().map(this::toResponse).toList();
    }

    @Transactional
    public BookingResponse approve(UUID bookingId, UUID currentUserId, boolean admin, BookingDecisionRequest request) {
        BookingEntity entity = findBooking(bookingId);
        requireApprover(entity, currentUserId, admin);
        return approveEntity(entity, trimToNull(request.note()));
    }

    @Transactional
    public BookingResponse reject(UUID bookingId, UUID currentUserId, boolean admin, BookingDecisionRequest request) {
        BookingEntity entity = findBooking(bookingId);
        requireApprover(entity, currentUserId, admin);
        return rejectEntity(entity, trimToNull(request.note()));
    }

    @Transactional
    public BookingResponse applyWorkflowDecision(UUID bookingId, String decision, String note) {
        BookingEntity entity = findBooking(bookingId);
        return switch (decision.trim().toUpperCase(Locale.ROOT)) {
            case "APPROVED" -> approveEntity(entity, trimToNull(note));
            case "REJECTED" -> rejectEntity(entity, trimToNull(note));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid workflow decision");
        };
    }

    private BookingResponse approveEntity(BookingEntity entity, String note) {
        if (entity.getStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending bookings can be approved");
        }

        BookingEntity saved = bookingTransitionService.approve(entity, note, OCCUPYING_STATUSES);
        return toResponse(saved);
    }

    private BookingResponse rejectEntity(BookingEntity entity, String note) {
        BookingEntity saved = bookingTransitionService.reject(entity, note);
        BookingResponse response = toResponse(saved);
        waitlistPromotionService.promote(saved.getResourceId(), OCCUPYING_STATUSES);
        return response;
    }

    private void validateWindow(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking end time must be after start time");
        }
        if (!startAt.isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking start time must be in the future");
        }
        LocalDate startDate = startAt.toLocalDate();
        LocalDate endDate = endAt.toLocalDate();
        if (!startDate.equals(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bookings must stay within a single day in this version");
        }
    }

    private void ensureBookableResource(ResourceClient.ResourceSnapshot resource, OffsetDateTime startAt, OffsetDateTime endAt) {
        if (!"ACTIVE".equalsIgnoreCase(resource.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resource is not active");
        }
        long durationMinutes = Duration.between(startAt, endAt).toMinutes();
        if (durationMinutes > resource.maxBookingDurationMinutes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking duration exceeds the resource policy");
        }
        if (startAt.isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusDays(resource.advanceBookingWindowDays()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is outside the allowed advance booking window");
        }
        int dayOfWeek = startAt.getDayOfWeek().getValue();
        boolean coveredByAvailability = resource.availabilityRules() != null && resource.availabilityRules().stream()
                .filter(ResourceClient.AvailabilityRuleSnapshot::available)
                .anyMatch(rule -> rule.dayOfWeek() == dayOfWeek
                        && !startAt.toLocalTime().isBefore(rule.startTime())
                        && !endAt.toLocalTime().isAfter(rule.endTime()));
        if (!coveredByAvailability) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requested time is outside the resource availability rules");
        }
        boolean overlapsMaintenance = resource.maintenanceSlots() != null && resource.maintenanceSlots().stream()
                .filter(slot -> "SCHEDULED".equalsIgnoreCase(slot.status()))
                .anyMatch(slot -> startAt.isBefore(slot.endsAt()) && endAt.isAfter(slot.startsAt()));
        if (overlapsMaintenance) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requested time overlaps a maintenance window");
        }
    }

    private void ensureLinkableEvent(UUID eventId) {
        EventClient.EventSnapshot event = eventClient.getEvent(eventId);
        if ("CANCELLED".equalsIgnoreCase(event.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled events cannot be linked to new bookings");
        }
    }

    private int nextWaitlistPosition(UUID resourceId) {
        return bookingRepository.findWaitlistedBookings(resourceId).stream()
                .map(BookingEntity::getWaitlistPosition)
                .filter(position -> position != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private void acquireResourceLock(UUID resourceId) {
        bookingLockBootstrapService.ensureLockExists(resourceId);
        bookingLockRepository.lockByResourceId(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to acquire booking lock"));
    }

    private void storeIdempotencyRecord(BookingEntity entity, UUID userId, String idempotencyKey, OffsetDateTime now) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        try {
            IdempotencyRecordEntity record = new IdempotencyRecordEntity();
            record.setIdempotencyKey(idempotencyKey.trim());
            record.setUserId(userId);
            record.setBookingId(entity.getBookingId());
            record.setCreatedAt(now);
            idempotencyRecordRepository.save(record);
        } catch (DataIntegrityViolationException ignored) {
        }
    }

    private void requireApprover(BookingEntity entity, UUID currentUserId, boolean admin) {
        if (admin) {
            return;
        }
        if (!entity.getResourceManagerId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Booking approval access denied");
        }
    }

    private void requireOwnerManagerOrAdmin(BookingEntity entity, UUID currentUserId, boolean admin) {
        if (admin || entity.getUserId().equals(currentUserId) || entity.getResourceManagerId().equals(currentUserId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Booking access denied");
    }

    private BookingEntity findBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private BookingStatus parseStatus(String raw) {
        try {
            return BookingStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid booking status");
        }
    }

    private ApprovalMode parseApprovalMode(String raw) {
        try {
            return ApprovalMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resource service returned an invalid approval mode");
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

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
