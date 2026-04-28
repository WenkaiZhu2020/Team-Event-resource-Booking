package com.teamresource.booking.service;

import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.api.dto.CreateBookingRequest;
import com.teamresource.booking.config.ClientProperties;
import com.teamresource.booking.domain.ApprovalMode;
import com.teamresource.booking.domain.BookingStatus;
import com.teamresource.booking.infra.client.EventClient;
import com.teamresource.booking.infra.client.ResourceClient;
import com.teamresource.booking.infra.client.WorkflowClient;
import com.teamresource.booking.infra.persistence.BookingEntity;
import com.teamresource.booking.infra.persistence.BookingLockEntity;
import com.teamresource.booking.infra.persistence.BookingLockRepository;
import com.teamresource.booking.infra.persistence.BookingRepository;
import com.teamresource.booking.infra.persistence.IdempotencyRecordRepository;
import com.teamresource.booking.infra.persistence.OutboxMessageRepository;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingLockRepository bookingLockRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    private StubResourceClient resourceClient;
    private StubEventClient eventClient;
    private StubWorkflowClient workflowClient;
    private StubBookingOutboxService bookingOutboxService;
    private BookingTransitionService bookingTransitionService;
    private WaitlistPromotionService waitlistPromotionService;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        resourceClient = new StubResourceClient();
        eventClient = new StubEventClient();
        workflowClient = new StubWorkflowClient();
        bookingOutboxService = new StubBookingOutboxService();
        bookingTransitionService = new BookingTransitionService(
                bookingRepository,
                bookingLockRepository,
                bookingOutboxService
        );
        waitlistPromotionService = new WaitlistPromotionService(
                bookingRepository,
                workflowClient,
                bookingOutboxService
        );
        bookingService = new BookingService(
                bookingRepository,
                bookingLockRepository,
                idempotencyRecordRepository,
                resourceClient,
                eventClient,
                workflowClient,
                bookingOutboxService,
                bookingTransitionService,
                waitlistPromotionService
        );
    }

    @Test
    void createShouldTriggerWorkflowForPendingApproval() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).withHour(10).withMinute(0);
        OffsetDateTime endAt = startAt.plusHours(2);

        when(idempotencyRecordRepository.findByIdempotencyKeyAndUserId("key-1", userId)).thenReturn(Optional.empty());
        resourceClient.snapshot = resourceSnapshot(resourceId, managerId, "MANAGER_APPROVAL", true);
        when(bookingLockRepository.lockByResourceId(resourceId)).thenReturn(Optional.of(new BookingLockEntity()));
        when(bookingRepository.findOverlappingBookings(resourceId, startAt, endAt, Set.of(BookingStatus.APPROVED, BookingStatus.PENDING_APPROVAL)))
                .thenReturn(List.of());
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.create(userId, new CreateBookingRequest(
                resourceId,
                null,
                startAt,
                endAt,
                "Team planning"
        ), "key-1");

        ArgumentCaptor<BookingEntity> captor = ArgumentCaptor.forClass(BookingEntity.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BookingStatus.PENDING_APPROVAL);
        assertThat(response.status()).isEqualTo("PENDING_APPROVAL");
        assertThat(workflowClient.lastApprovalBookingId).isEqualTo(response.bookingId());
        assertThat(bookingOutboxService.lastEventType).isEqualTo("booking.created");
    }

    @Test
    void applyWorkflowDecisionShouldApprovePendingBooking() {
        UUID bookingId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        BookingEntity entity = new BookingEntity();
        entity.setBookingId(bookingId);
        entity.setUserId(UUID.randomUUID());
        entity.setResourceId(resourceId);
        entity.setResourceName("Room A");
        entity.setResourceManagerId(UUID.randomUUID());
        entity.setResourceType("ROOM");
        entity.setPurpose("Review session");
        entity.setStartAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(3));
        entity.setEndAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).plusHours(1));
        entity.setApprovalMode(ApprovalMode.MANAGER_APPROVAL);
        entity.setStatus(BookingStatus.PENDING_APPROVAL);
        entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(entity));
        when(bookingLockRepository.lockByResourceId(resourceId)).thenReturn(Optional.of(new BookingLockEntity()));
        when(bookingRepository.findOverlappingBookings(resourceId, entity.getStartAt(), entity.getEndAt(), Set.of(BookingStatus.APPROVED, BookingStatus.PENDING_APPROVAL)))
                .thenReturn(List.of(entity));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.applyWorkflowDecision(bookingId, "APPROVED", "Looks good");

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.decisionNote()).isEqualTo("Looks good");
        assertThat(bookingOutboxService.lastEventType).isEqualTo("booking.approved");
    }

    @Test
    void applyWorkflowDecisionShouldRejectUnsupportedDecision() {
        UUID bookingId = UUID.randomUUID();
        BookingEntity entity = new BookingEntity();
        entity.setBookingId(bookingId);
        entity.setStatus(BookingStatus.PENDING_APPROVAL);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> bookingService.applyWorkflowDecision(bookingId, "MAYBE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(bookingRepository, never()).save(any());
    }

    private ResourceClient.ResourceSnapshot resourceSnapshot(UUID resourceId, UUID managerId, String approvalMode, boolean allowWaitlist) {
        return new ResourceClient.ResourceSnapshot(
                resourceId,
                managerId,
                "Room A",
                "Large room",
                "ROOM",
                "Building 1",
                10,
                "ACTIVE",
                approvalMode,
                true,
                allowWaitlist,
                240,
                30,
                List.of(
                        new ResourceClient.AvailabilityRuleSnapshot(UUID.randomUUID(), 1, LocalTime.of(8, 0), LocalTime.of(18, 0), true),
                        new ResourceClient.AvailabilityRuleSnapshot(UUID.randomUUID(), 2, LocalTime.of(8, 0), LocalTime.of(18, 0), true),
                        new ResourceClient.AvailabilityRuleSnapshot(UUID.randomUUID(), 3, LocalTime.of(8, 0), LocalTime.of(18, 0), true),
                        new ResourceClient.AvailabilityRuleSnapshot(UUID.randomUUID(), 4, LocalTime.of(8, 0), LocalTime.of(18, 0), true),
                        new ResourceClient.AvailabilityRuleSnapshot(UUID.randomUUID(), 5, LocalTime.of(8, 0), LocalTime.of(18, 0), true),
                        new ResourceClient.AvailabilityRuleSnapshot(UUID.randomUUID(), 6, LocalTime.of(8, 0), LocalTime.of(18, 0), true),
                        new ResourceClient.AvailabilityRuleSnapshot(UUID.randomUUID(), 7, LocalTime.of(8, 0), LocalTime.of(18, 0), true)
                ),
                List.of(),
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private static class StubResourceClient extends ResourceClient {
        private ResourceSnapshot snapshot;

        StubResourceClient() {
            super(RestClient.create());
        }

        @Override
        public ResourceSnapshot getResource(UUID resourceId) {
            return snapshot;
        }
    }

    private static class StubEventClient extends EventClient {
        StubEventClient() {
            super(RestClient.create());
        }
    }

    private static class StubWorkflowClient extends WorkflowClient {
        private UUID lastApprovalBookingId;

        StubWorkflowClient() {
            super(RestClient.create(), new ClientProperties(null, null, null, null, null));
        }

        @Override
        public void createBookingApproval(BookingResponse booking) {
            this.lastApprovalBookingId = booking.bookingId();
        }
    }

    private class StubBookingOutboxService extends BookingOutboxService {
        private String lastEventType;

        StubBookingOutboxService() {
            super(outboxMessageRepository, new com.fasterxml.jackson.databind.ObjectMapper());
        }

        @Override
        public void record(String eventType, BookingResponse response) {
            this.lastEventType = eventType;
        }
    }
}
