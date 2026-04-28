package com.teamresource.booking.application.service;

import com.teamresource.booking.application.command.ApproveBookingCommand;
import com.teamresource.booking.application.command.CancelBookingCommand;
import com.teamresource.booking.application.command.RejectBookingCommand;
import com.teamresource.booking.common.error.ApiException;
import com.teamresource.booking.domain.event.DomainEventPublisher;
import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.domain.model.BookingSearchCriteria;
import com.teamresource.booking.domain.model.BookingStatus;
import com.teamresource.booking.domain.model.WaitlistStatus;
import com.teamresource.booking.domain.repository.BookingRepository;
import com.teamresource.booking.domain.repository.ResourceBookingLockRepository;
import com.teamresource.booking.domain.repository.WaitlistRepository;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import com.teamresource.booking.infrastructure.persistence.entity.WaitlistEntryEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingManagementService {

    private final BookingRepository bookingRepository;
    private final WaitlistRepository waitlistRepository;
    private final ResourceBookingLockRepository lockRepository;
    private final BookingTransitionService transitionService;
    private final BookingEventFactory eventFactory;
    private final DomainEventPublisher domainEventPublisher;

    public BookingManagementService(
            BookingRepository bookingRepository,
            WaitlistRepository waitlistRepository,
            ResourceBookingLockRepository lockRepository,
            BookingTransitionService transitionService,
            BookingEventFactory eventFactory,
            DomainEventPublisher domainEventPublisher
    ) {
        this.bookingRepository = bookingRepository;
        this.waitlistRepository = waitlistRepository;
        this.lockRepository = lockRepository;
        this.transitionService = transitionService;
        this.eventFactory = eventFactory;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional(readOnly = true)
    public BookingEntity getBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException("BOOKING_NOT_FOUND", HttpStatus.NOT_FOUND, "Booking not found"));
    }

    @Transactional(readOnly = true)
    public Page<BookingEntity> search(BookingSearchCriteria criteria, Pageable pageable) {
        return bookingRepository.search(criteria, pageable);
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryEntity> listWaitlist(UUID resourceId) {
        return waitlistRepository.findByResourceId(resourceId, WaitlistStatus.WAITING);
    }

    @Transactional
    public BookingEntity cancel(CancelBookingCommand command) {
        BookingEntity booking = getBooking(command.bookingId());
        if (!command.admin() && !booking.getUserId().equals(command.actorUserId())) {
            throw new ApiException("BOOKING_CANCEL_FORBIDDEN", HttpStatus.FORBIDDEN, "Only booking owner or admin can cancel booking");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        BookingStatus previousStatus = booking.getStatus();

        String eventType = transitionService.cancel(booking, command.reason().trim(), now);
        BookingEntity saved = bookingRepository.save(booking);

        WaitlistEntryEntity waitlistEntry = waitlistRepository.findByBookingId(saved.getId()).orElse(null);
        if (waitlistEntry != null && waitlistEntry.getStatus() == WaitlistStatus.WAITING) {
            waitlistEntry.setStatus(WaitlistStatus.CANCELLED);
            waitlistEntry.setUpdatedAt(now);
            waitlistRepository.save(waitlistEntry);
        }

        domainEventPublisher.publish(eventFactory.from(eventType, saved, command.reason(), now));

        if (previousStatus == BookingStatus.APPROVED || previousStatus == BookingStatus.PENDING_APPROVAL) {
            promoteFromWaitlist(saved.getResourceId(), saved.getStartAt(), saved.getEndAt(), now);
        }

        return saved;
    }

    @Transactional
    public BookingEntity approve(ApproveBookingCommand command) {
        BookingEntity booking = getBooking(command.bookingId());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        String eventType = transitionService.approve(booking, command.approverUserId(), now);
        BookingEntity saved = bookingRepository.save(booking);

        domainEventPublisher.publish(eventFactory.from(eventType, saved, null, now));
        return saved;
    }

    @Transactional
    public BookingEntity reject(RejectBookingCommand command) {
        BookingEntity booking = getBooking(command.bookingId());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        BookingStatus previousStatus = booking.getStatus();

        String eventType = transitionService.reject(booking, command.approverUserId(), command.reason().trim(), now);
        BookingEntity saved = bookingRepository.save(booking);

        domainEventPublisher.publish(eventFactory.from(eventType, saved, command.reason(), now));
        if (previousStatus == BookingStatus.PENDING_APPROVAL) {
            promoteFromWaitlist(saved.getResourceId(), saved.getStartAt(), saved.getEndAt(), now);
        }
        return saved;
    }

    @Transactional
    public void cancelFutureByEvent(UUID eventId, String reason) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<BookingEntity> bookings = bookingRepository.findFutureByEvent(eventId, now);
        for (BookingEntity booking : bookings) {
            if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
                continue;
            }
            transitionService.cancel(booking, reason, now);
            BookingEntity saved = bookingRepository.save(booking);
            domainEventPublisher.publish(eventFactory.from("BOOKING_CANCELLED_BY_EVENT", saved, reason, now));
        }
    }

    @Transactional
    public void cancelFutureByResource(UUID resourceId, String reason) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<BookingEntity> bookings = bookingRepository.findFutureByResource(resourceId, now);
        for (BookingEntity booking : bookings) {
            if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
                continue;
            }
            transitionService.cancel(booking, reason, now);
            BookingEntity saved = bookingRepository.save(booking);
            domainEventPublisher.publish(eventFactory.from("BOOKING_CANCELLED_BY_RESOURCE", saved, reason, now));
        }
    }

    private void promoteFromWaitlist(UUID resourceId, OffsetDateTime startAt, OffsetDateTime endAt, OffsetDateTime now) {
        lockRepository.acquireLock(resourceId);

        boolean conflict = bookingRepository.existsOverlappingActiveBooking(
                resourceId,
                startAt,
                endAt,
                List.of(BookingStatus.APPROVED, BookingStatus.PENDING_APPROVAL)
        );
        if (conflict) {
            return;
        }

        WaitlistEntryEntity next = waitlistRepository.findNextWaiting(resourceId, startAt, endAt).orElse(null);
        if (next == null) {
            return;
        }

        BookingEntity booking = bookingRepository.findById(next.getBookingId()).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.WAITLISTED) {
            next.setStatus(WaitlistStatus.EXPIRED);
            next.setUpdatedAt(now);
            waitlistRepository.save(next);
            return;
        }

        if (booking.isApprovalRequired()) {
            booking.setStatus(BookingStatus.PENDING_APPROVAL);
            booking.setApprovalStatus(ApprovalStatus.PENDING);
        } else {
            booking.setStatus(BookingStatus.APPROVED);
            booking.setApprovalStatus(ApprovalStatus.NOT_REQUIRED);
            booking.setConfirmedAt(now);
        }

        BookingEntity saved = bookingRepository.save(booking);

        next.setStatus(WaitlistStatus.PROMOTED);
        next.setPromotedAt(now);
        next.setUpdatedAt(now);
        waitlistRepository.save(next);

        if (saved.isApprovalRequired()) {
            domainEventPublisher.publish(eventFactory.from("BOOKING_APPROVAL_REQUESTED", saved, "waitlist-promoted", now));
        } else {
            domainEventPublisher.publish(eventFactory.from("BOOKING_APPROVED", saved, "waitlist-promoted", now));
        }
        domainEventPublisher.publish(eventFactory.from("BOOKING_WAITLIST_PROMOTED", saved, null, now));
    }
}
