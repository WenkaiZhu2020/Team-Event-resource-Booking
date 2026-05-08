package com.teamresource.booking.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.booking.infra.persistence.ConsumedMessageEntity;
import com.teamresource.booking.infra.persistence.ConsumedMessageRepository;
import com.teamresource.booking.service.BookingSagaCompensationService;
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
public class BookingSagaCompensationEventConsumer {

    private final ObjectMapper objectMapper;
    private final BookingSagaCompensationService compensationService;
    private final ConsumedMessageRepository consumedMessageRepository;

    public BookingSagaCompensationEventConsumer(
            ObjectMapper objectMapper,
            BookingSagaCompensationService compensationService,
            ConsumedMessageRepository consumedMessageRepository
    ) {
        this.objectMapper = objectMapper;
        this.compensationService = compensationService;
        this.consumedMessageRepository = consumedMessageRepository;
    }

    @RabbitListener(queues = "${app.saga.compensation-queue:booking.saga.compensation}")
    @Transactional
    public void onMessage(Message message) {
        String source = "booking-saga";
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
            String eventType = readText(envelope, "eventType");
            if (eventType == null || eventType.isBlank()) {
                eventType = message.getMessageProperties().getReceivedRoutingKey();
            }
            JsonNode payload = envelope.path("payload");

            if (isWorkflowApprovalRejected(eventType)) {
                UUID bookingId = bookingIdFrom(envelope, payload);
                if (bookingId != null) {
                    compensationService.compensateWorkflowApprovalRejected(bookingId, reasonFrom(payload, "Workflow approval rejected"));
                }
            } else if (isResourceAllocationFailed(eventType)) {
                UUID bookingId = bookingIdFrom(envelope, payload);
                if (bookingId != null) {
                    compensationService.compensateResourceAllocationFailure(bookingId, reasonFrom(payload, "Resource allocation failed"));
                }
            }

            ConsumedMessageEntity consumed = new ConsumedMessageEntity();
            consumed.setId(UUID.randomUUID());
            consumed.setSource(source);
            consumed.setMessageId(messageId);
            consumed.setConsumedAt(OffsetDateTime.now(ZoneOffset.UTC));
            consumed.setPayloadHash(payloadHash(body));
            consumedMessageRepository.save(consumed);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to process saga compensation event", ex);
        }
    }

    private boolean isWorkflowApprovalRejected(String eventType) {
        return "workflow.approval.rejected".equalsIgnoreCase(eventType)
                || "WorkflowApprovalRejectedEvent".equalsIgnoreCase(eventType)
                || "WORKFLOW_APPROVAL_REJECTED".equalsIgnoreCase(eventType);
    }

    private boolean isResourceAllocationFailed(String eventType) {
        return "resource.allocation.failed".equalsIgnoreCase(eventType)
                || "ResourceAllocationFailedEvent".equalsIgnoreCase(eventType)
                || "RESOURCE_ALLOCATION_FAILED".equalsIgnoreCase(eventType);
    }

    private UUID bookingIdFrom(JsonNode envelope, JsonNode payload) {
        UUID bookingId = uuid(payload, "bookingId");
        if (bookingId != null) {
            return bookingId;
        }
        bookingId = uuid(payload, "targetId");
        if (bookingId != null) {
            return bookingId;
        }
        bookingId = uuid(envelope, "aggregateId");
        if (bookingId != null) {
            return bookingId;
        }
        return uuid(payload, "aggregateId");
    }

    private String reasonFrom(JsonNode payload, String fallback) {
        String reason = readText(payload, "reason");
        if (reason != null && !reason.isBlank()) {
            return reason;
        }
        reason = readText(payload, "decisionNote");
        if (reason != null && !reason.isBlank()) {
            return reason;
        }
        return fallback;
    }

    private UUID uuid(JsonNode node, String field) {
        String value = readText(node, field);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return text == null || text.isBlank() ? null : text;
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
