package com.teamresource.workflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.workflow.domain.ApprovalTargetType;
import com.teamresource.workflow.infra.persistence.ConsumedMessageEntity;
import com.teamresource.workflow.infra.persistence.ConsumedMessageRepository;
import com.teamresource.workflow.service.command.CreateApprovalCommand;
import com.teamresource.workflow.service.observer.DomainEventObserver;
import com.teamresource.workflow.service.observer.DomainEventObserverDispatcher;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingDomainEventConsumerTest {

    @Mock
    private ConsumedMessageRepository consumedMessageRepository;

    private RecordingApprovalWorkflowFacade approvalWorkflowFacade;
    private BookingDomainEventConsumer consumer;
    private RecordingObserver observer;

    @BeforeEach
    void setUp() {
        approvalWorkflowFacade = new RecordingApprovalWorkflowFacade();
        observer = new RecordingObserver();
        DomainEventObserverDispatcher dispatcher = new DomainEventObserverDispatcher(List.of(observer));
        consumer = new BookingDomainEventConsumer(new ObjectMapper(), dispatcher, approvalWorkflowFacade, consumedMessageRepository);
    }

    @Test
    void onMessageShouldDispatchCommandsAndPersistConsumptionMarker() {
        String body = """
                {
                  "eventType":"booking.created",
                  "payload":{
                    "bookingId":"8cb35f42-ef44-4d80-b290-ec0338d7bb5b",
                    "userId":"c54b4f43-819f-47b5-a2dc-2fe3b26c4d43",
                    "resourceId":"fda558d9-dbd4-4594-ac1e-5212ec23c34f",
                    "resourceManagerId":"44f8bd09-7211-4792-a930-f5329c8a5f07",
                    "resourceName":"Innovation Lab",
                    "status":"PENDING_APPROVAL",
                    "startAt":"2026-06-01T09:00:00Z",
                    "endAt":"2026-06-01T11:00:00Z"
                  }
                }
                """;
        MessageProperties properties = new MessageProperties();
        properties.setMessageId("msg-1");
        Message message = new Message(body.getBytes(StandardCharsets.UTF_8), properties);

        when(consumedMessageRepository.existsBySourceAndMessageId("booking-domain", "msg-1")).thenReturn(false);
        when(consumedMessageRepository.save(org.mockito.ArgumentMatchers.any(ConsumedMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        consumer.onMessage(message);

        assertThat(observer.lastEventType).isEqualTo("booking.created");
        assertThat(approvalWorkflowFacade.lastCommand).isNotNull();
        ArgumentCaptor<ConsumedMessageEntity> captor = ArgumentCaptor.forClass(ConsumedMessageEntity.class);
        verify(consumedMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo("booking-domain");
        assertThat(captor.getValue().getMessageId()).isEqualTo("msg-1");
    }

    @Test
    void onMessageShouldIgnoreAlreadyConsumedMessage() {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId("msg-2");
        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8), properties);
        when(consumedMessageRepository.existsBySourceAndMessageId("booking-domain", "msg-2")).thenReturn(true);

        consumer.onMessage(message);

        assertThat(observer.lastEventType).isNull();
        verify(consumedMessageRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static class RecordingApprovalWorkflowFacade extends ApprovalWorkflowFacade {
        private CreateApprovalCommand lastCommand;

        RecordingApprovalWorkflowFacade() {
            super(null, null, null, null);
        }

        @Override
        public com.teamresource.workflow.api.dto.ApprovalResponse create(CreateApprovalCommand command) {
            this.lastCommand = command;
            return null;
        }
    }

    private static class RecordingObserver implements DomainEventObserver {
        private String lastEventType;

        @Override
        public boolean supports(String eventType) {
            return "booking.created".equals(eventType);
        }

        @Override
        public List<CreateApprovalCommand> onEvent(String source, String messageId, JsonNode payload) {
            this.lastEventType = "booking.created";
            return List.of(new CreateApprovalCommand(
                    ApprovalTargetType.BOOKING,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Booking approval",
                    null,
                    "summary",
                    null,
                    null,
                    null
            ));
        }
    }
}
