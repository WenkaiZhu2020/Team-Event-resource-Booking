package com.teamresource.booking.service;

import com.teamresource.booking.domain.ApprovalMode;
import com.teamresource.booking.domain.BookingStatus;
import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.infra.persistence.BookingEntity;
import com.teamresource.booking.infra.persistence.BookingRepository;
import com.teamresource.booking.lock.ResourceLockService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingTransitionServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    private TrackingBookingOutboxService bookingOutboxService;
    private TrackingBookingOutboxPublisher bookingOutboxPublisher;

    private BookingTransitionService bookingTransitionService;

    @BeforeEach
    void setUp() {
        bookingOutboxService = new TrackingBookingOutboxService();
        bookingOutboxPublisher = new TrackingBookingOutboxPublisher();
        bookingTransitionService = new BookingTransitionService(
                bookingRepository,
                new PassThroughResourceLockService(),
                bookingOutboxService,
                bookingOutboxPublisher
        );
    }

    @Test
    void compensateResourceAllocationFailureShouldCancelPendingBookingAndWriteCompensationEvent() {
        BookingEntity entity = pendingBooking();
        when(bookingRepository.findById(entity.getBookingId())).thenReturn(java.util.Optional.of(entity));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingEntity saved = bookingTransitionService.compensateResourceAllocationFailure(entity, "Inventory reservation failed");

        assertThat(saved.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(saved.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(saved.getDecisionNote()).isEqualTo("Inventory reservation failed");
        assertThat(saved.getCancelledAt()).isNotNull();
        assertThat(saved.getCancellationReason()).isEqualTo("Inventory reservation failed");

        assertThat(bookingOutboxPublisher.lastCompensationEvent).isNotNull();
        assertThat(bookingOutboxPublisher.lastCompensationEvent.bookingId()).isEqualTo(entity.getBookingId());
        assertThat(bookingOutboxPublisher.lastCompensationEvent.resourceId()).isEqualTo(entity.getResourceId());
        assertThat(bookingOutboxPublisher.lastCompensationEvent.previousStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(bookingOutboxPublisher.lastCompensationEvent.compensatedStatus()).isEqualTo("CANCELLED");
        assertThat(bookingOutboxPublisher.lastCompensationEvent.compensationSource()).isEqualTo("RESOURCE_ALLOCATION_FAILED");
        assertThat(bookingOutboxPublisher.lastCompensationEvent.reason()).isEqualTo("Inventory reservation failed");
        assertThat(bookingOutboxService.lastEventType).isNull();
    }

    @Test
    void compensateWorkflowApprovalRejectedShouldRejectPendingBookingAndWriteCompensationEvent() {
        BookingEntity entity = pendingBooking();
        when(bookingRepository.findById(entity.getBookingId())).thenReturn(java.util.Optional.of(entity));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingEntity saved = bookingTransitionService.compensateWorkflowApprovalRejected(entity, "Manager rejected booking");

        assertThat(saved.getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertThat(saved.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(saved.getRejectedAt()).isNotNull();
        assertThat(saved.getRejectionReason()).isEqualTo("Manager rejected booking");

        assertThat(bookingOutboxPublisher.lastCompensationEvent).isNotNull();
        assertThat(bookingOutboxPublisher.lastCompensationEvent.previousStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(bookingOutboxPublisher.lastCompensationEvent.compensatedStatus()).isEqualTo("REJECTED");
        assertThat(bookingOutboxPublisher.lastCompensationEvent.compensationSource()).isEqualTo("WORKFLOW_APPROVAL_REJECTED");
    }

    @Test
    void compensateShouldBeNoOpWhenRefetchedEntityIsNoLongerPending() {
        BookingEntity staleEntity = pendingBooking();
        BookingEntity currentEntity = pendingBooking();
        currentEntity.setBookingId(staleEntity.getBookingId());
        currentEntity.setResourceId(staleEntity.getResourceId());
        currentEntity.setStatus(BookingStatus.APPROVED);
        when(bookingRepository.findById(staleEntity.getBookingId())).thenReturn(java.util.Optional.of(currentEntity));

        BookingEntity returned = bookingTransitionService.compensateResourceAllocationFailure(staleEntity, "late failure");

        assertThat(returned).isSameAs(currentEntity);
        verify(bookingRepository, never()).save(any());
        assertThat(bookingOutboxPublisher.lastCompensationEvent).isNull();
    }

    @Test
    void compensateShouldBeNoOpForNonPendingBooking() {
        BookingEntity entity = pendingBooking();
        entity.setStatus(BookingStatus.APPROVED);

        BookingEntity returned = bookingTransitionService.compensateResourceAllocationFailure(entity, "late failure");

        assertThat(returned).isSameAs(entity);
        verify(bookingRepository, never()).save(any());
        assertThat(bookingOutboxPublisher.lastCompensationEvent).isNull();
    }

    private BookingEntity pendingBooking() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        BookingEntity entity = new BookingEntity();
        entity.setBookingId(UUID.randomUUID());
        entity.setUserId(UUID.randomUUID());
        entity.setResourceId(UUID.randomUUID());
        entity.setResourceName("Room A");
        entity.setResourceManagerId(UUID.randomUUID());
        entity.setResourceType("ROOM");
        entity.setPurpose("Quarterly planning");
        entity.setApprovalMode(ApprovalMode.MANAGER_APPROVAL);
        entity.setStatus(BookingStatus.PENDING_APPROVAL);
        entity.setApprovalStatus(ApprovalStatus.PENDING);
        entity.setStartAt(now.plusDays(1));
        entity.setEndAt(now.plusDays(1).plusHours(1));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCorrelationId("corr-123");
        return entity;
    }

    private static class PassThroughResourceLockService implements ResourceLockService {
        @Override
        public <T> T executeWithResourceLock(UUID resourceId, java.util.function.Supplier<T> action) {
            return action.get();
        }
    }

    private static class TrackingBookingOutboxService extends BookingOutboxService {
        private String lastEventType;

        TrackingBookingOutboxService() {
            super(null, new com.fasterxml.jackson.databind.ObjectMapper());
        }

        @Override
        public void record(String eventType, com.teamresource.booking.api.dto.BookingResponse response) {
            this.lastEventType = eventType;
        }
    }

    private static class TrackingBookingOutboxPublisher extends BookingOutboxPublisher {
        private BookingCompensatedEvent lastCompensationEvent;

        TrackingBookingOutboxPublisher() {
            super(null, null, null);
        }

        @Override
        public void enqueueCompensatedEvent(BookingCompensatedEvent event) {
            this.lastCompensationEvent = event;
        }
    }
}
