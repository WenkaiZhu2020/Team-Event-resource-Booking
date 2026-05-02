package com.teamresource.booking.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.booking.domain.repository.ConsumedMessageRepository;
import com.teamresource.booking.infrastructure.persistence.entity.ConsumedMessageEntity;
import com.teamresource.booking.service.BookingSagaCompensationService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingSagaCompensationEventConsumerTest {

    @Mock
    private ConsumedMessageRepository consumedMessageRepository;

    private TrackingBookingSagaCompensationService compensationService;
    private BookingSagaCompensationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        compensationService = new TrackingBookingSagaCompensationService();
        consumer = new BookingSagaCompensationEventConsumer(
                new ObjectMapper(),
                compensationService,
                consumedMessageRepository
        );
    }

    @Test
    void shouldCompensateWorkflowRejectionUsingPayloadTargetId() {
        UUID bookingId = UUID.randomUUID();
        Message message = MessageBuilder.withBody(("""
                {
                  "eventType":"workflow.approval.rejected",
                  "payload":{
                    "targetId":"%s",
                    "decisionNote":"Manager rejected"
                  }
                }
                """.formatted(bookingId)).getBytes(StandardCharsets.UTF_8))
                .setMessageId("msg-1")
                .build();
        when(consumedMessageRepository.exists("booking-saga", "msg-1")).thenReturn(false);

        consumer.onMessage(message);

        assertThat(compensationService.workflowRejectedBookingId).isEqualTo(bookingId);
        assertThat(compensationService.workflowRejectedReason).isEqualTo("Manager rejected");
        ArgumentCaptor<ConsumedMessageEntity> consumedCaptor = ArgumentCaptor.forClass(ConsumedMessageEntity.class);
        verify(consumedMessageRepository).save(consumedCaptor.capture());
        assertThat(consumedCaptor.getValue().getMessageId()).isEqualTo("msg-1");
    }

    @Test
    void shouldCompensateResourceFailureUsingRoutingKeyFallback() {
        UUID bookingId = UUID.randomUUID();
        Message message = MessageBuilder.withBody(("""
                {
                  "payload":{
                    "bookingId":"%s",
                    "reason":"Allocation failed in resource-service"
                  }
                }
                """.formatted(bookingId)).getBytes(StandardCharsets.UTF_8))
                .setMessageId("msg-2")
                .setReceivedRoutingKey("resource.allocation.failed")
                .build();
        when(consumedMessageRepository.exists("booking-saga", "msg-2")).thenReturn(false);

        consumer.onMessage(message);

        assertThat(compensationService.resourceFailureBookingId).isEqualTo(bookingId);
        assertThat(compensationService.resourceFailureReason).isEqualTo("Allocation failed in resource-service");
    }

    @Test
    void shouldIgnoreDuplicateMessage() {
        Message message = MessageBuilder.withBody("{\"eventType\":\"workflow.approval.rejected\"}".getBytes(StandardCharsets.UTF_8))
                .setMessageId("msg-3")
                .build();
        when(consumedMessageRepository.exists("booking-saga", "msg-3")).thenReturn(true);

        consumer.onMessage(message);

        assertThat(compensationService.workflowRejectedBookingId).isNull();
        assertThat(compensationService.resourceFailureBookingId).isNull();
        verify(consumedMessageRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static class TrackingBookingSagaCompensationService extends BookingSagaCompensationService {
        private UUID workflowRejectedBookingId;
        private String workflowRejectedReason;
        private UUID resourceFailureBookingId;
        private String resourceFailureReason;

        TrackingBookingSagaCompensationService() {
            super(null, null);
        }

        @Override
        public void compensateResourceAllocationFailure(UUID bookingId, String reason) {
            this.resourceFailureBookingId = bookingId;
            this.resourceFailureReason = reason;
        }

        @Override
        public void compensateWorkflowApprovalRejected(UUID bookingId, String reason) {
            this.workflowRejectedBookingId = bookingId;
            this.workflowRejectedReason = reason;
        }
    }
}
