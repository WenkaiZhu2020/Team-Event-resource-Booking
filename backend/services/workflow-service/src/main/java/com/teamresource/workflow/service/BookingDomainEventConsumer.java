package com.teamresource.workflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.workflow.infra.persistence.ConsumedMessageEntity;
import com.teamresource.workflow.infra.persistence.ConsumedMessageRepository;
import com.teamresource.workflow.service.observer.DomainEventObserverDispatcher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookingDomainEventConsumer {

    private final ObjectMapper objectMapper;
    private final DomainEventObserverDispatcher observerDispatcher;
    private final ApprovalWorkflowFacade approvalWorkflowFacade;
    private final ConsumedMessageRepository consumedMessageRepository;

    public BookingDomainEventConsumer(
            ObjectMapper objectMapper,
            DomainEventObserverDispatcher observerDispatcher,
            ApprovalWorkflowFacade approvalWorkflowFacade,
            ConsumedMessageRepository consumedMessageRepository
    ) {
        this.objectMapper = objectMapper;
        this.observerDispatcher = observerDispatcher;
        this.approvalWorkflowFacade = approvalWorkflowFacade;
        this.consumedMessageRepository = consumedMessageRepository;
    }

    @RabbitListener(queues = "${app.messaging.consumers.booking-events-queue}")
    @Transactional
    public void onMessage(Message message) {
        String source = "booking-domain";
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String messageId = message.getMessageProperties().getMessageId();
        if (messageId == null || messageId.isBlank()) {
            messageId = payloadHash(body);
        }
        if (consumedMessageRepository.existsBySourceAndMessageId(source, messageId)) {
            return;
        }

        try {
            JsonNode envelope = objectMapper.readTree(body);
            String eventType = envelope.path("eventType").asText(null);
            JsonNode payload = envelope.path("payload");
            observerDispatcher.dispatch(source, messageId, eventType, payload)
                    .forEach(approvalWorkflowFacade::create);

            ConsumedMessageEntity consumed = new ConsumedMessageEntity();
            consumed.setConsumedMessageId(UUID.randomUUID());
            consumed.setSource(source);
            consumed.setMessageId(messageId);
            consumed.setPayloadHash(payloadHash(body));
            consumed.setConsumedAt(OffsetDateTime.now(ZoneOffset.UTC));
            consumedMessageRepository.save(consumed);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to process booking domain event", ex);
        }
    }

    private String payloadHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
