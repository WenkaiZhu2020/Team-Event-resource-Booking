package com.teamresource.booking.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.booking.application.client.ResourcePrecheckGateway;
import com.teamresource.booking.application.client.ResourcePrecheckResult;
import com.teamresource.booking.application.command.CreateBookingCommand;
import com.teamresource.booking.common.error.ApiException;
import com.teamresource.booking.domain.event.DomainEventPublisher;
import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.domain.model.BookingStatus;
import com.teamresource.booking.domain.model.IdempotencyStatus;
import com.teamresource.booking.domain.repository.BookingRepository;
import com.teamresource.booking.domain.repository.IdempotencyRepository;
import com.teamresource.booking.domain.repository.WaitlistRepository;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import com.teamresource.booking.infrastructure.persistence.entity.BookingIdempotencyKeyEntity;
import com.teamresource.booking.infrastructure.persistence.entity.WaitlistEntryEntity;
import com.teamresource.booking.lock.ResourceLockService;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateBookingCommandHandler {

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {
    };

    private final BookingRepository bookingRepository;
    private final WaitlistRepository waitlistRepository;
    private final ResourceLockService resourceLockService;
    private final IdempotencyRepository idempotencyRepository;
    private final ResourcePrecheckGateway resourcePrecheckGateway;
    private final DomainEventPublisher domainEventPublisher;
    private final BookingEventFactory bookingEventFactory;
    private final IdempotencyHashService hashService;
    private final ObjectMapper objectMapper;

    public CreateBookingCommandHandler(
            BookingRepository bookingRepository,
            WaitlistRepository waitlistRepository,
            ResourceLockService resourceLockService,
            IdempotencyRepository idempotencyRepository,
            ResourcePrecheckGateway resourcePrecheckGateway,
            DomainEventPublisher domainEventPublisher,
            BookingEventFactory bookingEventFactory,
            IdempotencyHashService hashService,
            ObjectMapper objectMapper
    ) {
        this.bookingRepository = bookingRepository;
        this.waitlistRepository = waitlistRepository;
        this.resourceLockService = resourceLockService;
        this.idempotencyRepository = idempotencyRepository;
        this.resourcePrecheckGateway = resourcePrecheckGateway;
        this.domainEventPublisher = domainEventPublisher;
        this.bookingEventFactory = bookingEventFactory;
        this.hashService = hashService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BookingEntity handle(CreateBookingCommand command) {
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new ApiException("IDEMPOTENCY_KEY_REQUIRED", HttpStatus.BAD_REQUEST, "Idempotency key is required");
        }
        if (!command.startAt().isBefore(command.endAt())) {
            throw new ApiException("BOOKING_TIME_RANGE_INVALID", HttpStatus.BAD_REQUEST, "startAt must be before endAt");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String requestHash = hashService.hash(command);

        BookingIdempotencyKeyEntity idempotency = beginIdempotency(command.idempotencyKey(), requestHash, now);
        if (idempotency.getStatus() == IdempotencyStatus.COMPLETED) {
            return getCompletedBooking(idempotency);
        }

        try {
            return resourceLockService.executeWithResourceLock(command.resourceId(), () -> {
                ResourcePrecheckResult precheck = resourcePrecheckGateway.precheck(command.resourceId(), command.startAt(), command.endAt());
                if (!precheck.bookingAllowed()) {
                    throw new ApiException(
                            "RESOURCE_PRECHECK_FAILED",
                            HttpStatus.CONFLICT,
                            "Booking is not allowed by resource policy: " + precheck.reasonCode()
                    );
                }

                boolean hasConflict = bookingRepository.existsOverlappingActiveBooking(
                        command.resourceId(),
                        command.startAt(),
                        command.endAt(),
                        List.of(BookingStatus.PENDING_APPROVAL, BookingStatus.APPROVED)
                );

                BookingEntity booking = new BookingEntity();
                booking.setId(UUID.randomUUID());
                booking.setUserId(command.userId());
                booking.setEventId(command.eventId());
                booking.setResourceId(command.resourceId());
                booking.setStartAt(command.startAt());
                booking.setEndAt(command.endAt());
                booking.setRequestedAt(now);
                booking.setCorrelationId(command.idempotencyKey());

                String statusEventType;
                if (hasConflict) {
                    if (!precheck.allowWaitlist()) {
                        throw new ApiException("BOOKING_CONFLICT", HttpStatus.CONFLICT, "Time slot already booked");
                    }
                    booking.setStatus(BookingStatus.WAITLISTED);
                    booking.setApprovalRequired(precheck.requiresApproval());
                    booking.setApprovalStatus(precheck.requiresApproval() ? ApprovalStatus.PENDING : ApprovalStatus.NOT_REQUIRED);
                    booking = bookingRepository.save(booking);

                    WaitlistEntryEntity entry = new WaitlistEntryEntity();
                    entry.setId(UUID.randomUUID());
                    entry.setBookingId(booking.getId());
                    entry.setResourceId(booking.getResourceId());
                    entry.setUserId(booking.getUserId());
                    entry.setStartAt(booking.getStartAt());
                    entry.setEndAt(booking.getEndAt());
                    entry.setPositionIndex(waitlistRepository.nextPosition(booking.getResourceId(), booking.getStartAt(), booking.getEndAt()));
                    entry.setStatus(com.teamresource.booking.domain.model.WaitlistStatus.WAITING);
                    entry.setCreatedAt(now);
                    entry.setUpdatedAt(now);
                    waitlistRepository.save(entry);

                    statusEventType = "BOOKING_WAITLISTED";
                } else {
                    booking.setApprovalRequired(precheck.requiresApproval());
                    if (precheck.requiresApproval()) {
                        booking.setStatus(BookingStatus.PENDING_APPROVAL);
                        booking.setApprovalStatus(ApprovalStatus.PENDING);
                        statusEventType = "BOOKING_APPROVAL_REQUESTED";
                    } else {
                        booking.setStatus(BookingStatus.APPROVED);
                        booking.setApprovalStatus(ApprovalStatus.NOT_REQUIRED);
                        booking.setConfirmedAt(now);
                        statusEventType = "BOOKING_APPROVED";
                    }
                    booking = bookingRepository.save(booking);
                }

                domainEventPublisher.publish(bookingEventFactory.from("BOOKING_CREATED", booking, null, now));
                domainEventPublisher.publish(bookingEventFactory.from(statusEventType, booking, null, now));

                idempotency.setStatus(IdempotencyStatus.COMPLETED);
                idempotency.setCompletedAt(now);
                idempotency.setResponseJson(writeResponseJson(booking.getId()));
                idempotencyRepository.save(idempotency);
                return booking;
            });
        } catch (RuntimeException ex) {
            idempotency.setStatus(IdempotencyStatus.FAILED);
            idempotency.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
            idempotencyRepository.save(idempotency);
            throw ex;
        }
    }

    private BookingIdempotencyKeyEntity beginIdempotency(String key, String requestHash, OffsetDateTime now) {
        BookingIdempotencyKeyEntity idempotency = idempotencyRepository.findByKey(key).orElse(null);
        if (idempotency == null) {
            BookingIdempotencyKeyEntity created = new BookingIdempotencyKeyEntity();
            created.setId(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
            created.setIdempotencyKey(key);
            created.setRequestHash(requestHash);
            created.setStatus(IdempotencyStatus.IN_PROGRESS);
            created.setCreatedAt(now);
            created.setExpiresAt(now.plusDays(1));
            try {
                return idempotencyRepository.save(created);
            } catch (DataIntegrityViolationException ex) {
                idempotency = idempotencyRepository.findByKey(key)
                        .orElseThrow(() -> new ApiException(
                                "IDEMPOTENCY_KEY_CREATE_CONFLICT",
                                HttpStatus.CONFLICT,
                                "Idempotency key is being processed concurrently"
                        ));
            }
        }

        if (!idempotency.getRequestHash().equals(requestHash)) {
            throw new ApiException("IDEMPOTENCY_KEY_CONFLICT", HttpStatus.CONFLICT, "Idempotency key reused with different request payload");
        }

        if (idempotency.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new ApiException("IDEMPOTENCY_KEY_IN_PROGRESS", HttpStatus.CONFLICT, "Request with same idempotency key is in progress");
        }

        if (idempotency.getStatus() == IdempotencyStatus.FAILED) {
            idempotency.setStatus(IdempotencyStatus.IN_PROGRESS);
            idempotency.setResponseJson(null);
            idempotency.setCompletedAt(null);
            idempotency.setExpiresAt(now.plusDays(1));
            return idempotencyRepository.save(idempotency);
        }

        return idempotency;
    }

    private BookingEntity getCompletedBooking(BookingIdempotencyKeyEntity idempotency) {
        try {
            Map<String, String> map = objectMapper.readValue(idempotency.getResponseJson(), MAP_TYPE);
            UUID bookingId = UUID.fromString(map.get("bookingId"));
            return bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ApiException("BOOKING_NOT_FOUND", HttpStatus.NOT_FOUND, "Booking not found for idempotency key"));
        } catch (Exception ex) {
            throw new ApiException("IDEMPOTENCY_RESPONSE_INVALID", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read idempotency response");
        }
    }

    private String writeResponseJson(UUID bookingId) {
        try {
            return objectMapper.writeValueAsString(Map.of("bookingId", bookingId.toString()));
        } catch (Exception ex) {
            throw new ApiException("IDEMPOTENCY_RESPONSE_WRITE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write idempotency response");
        }
    }
}
