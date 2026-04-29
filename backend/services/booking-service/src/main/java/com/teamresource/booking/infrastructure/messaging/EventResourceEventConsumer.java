package com.teamresource.booking.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.booking.application.service.BookingManagementService;
import com.teamresource.booking.domain.repository.ConsumedMessageRepository;
import com.teamresource.booking.infrastructure.persistence.entity.ConsumedMessageEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("stage2-layered-inactive")
public class EventResourceEventConsumer {

    private final ObjectMapper objectMapper;
    private final BookingManagementService bookingManagementService;
    private final ConsumedMessageRepository consumedMessageRepository;

    public EventResourceEventConsumer(
            ObjectMapper objectMapper,
            BookingManagementService bookingManagementService,
            ConsumedMessageRepository consumedMessageRepository
    ) {
        this.objectMapper = objectMapper;
        this.bookingManagementService = bookingManagementService;
        this.consumedMessageRepository = consumedMessageRepository;
    }

    @RabbitListener(queues = "${app.messaging.consumers.event-resource-queue}")
    @Transactional
    @SuppressWarnings("unchecked")
    public void onMessage(Message message) {
        String source = "event-resource";
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String messageId = message.getMessageProperties().getMessageId();
        if (messageId == null || messageId.isBlank()) {
            messageId = payloadHash(body);
        }

        if (consumedMessageRepository.exists(source, messageId)) {
            return;
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(body, Map.class);
            String eventType = string(payload.get("eventType"));

            if ("EVENT_CANCELLED".equalsIgnoreCase(eventType)) {
                UUID eventId = uuid(payload.get("eventId"));
                if (eventId != null) {
                    bookingManagementService.cancelFutureByEvent(eventId, "Event cancelled");
                }
            }

            if ("RESOURCE_RETIRED".equalsIgnoreCase(eventType) || "RESOURCE_DEACTIVATED".equalsIgnoreCase(eventType)) {
                UUID resourceId = uuid(payload.get("resourceId"));
                if (resourceId != null) {
                    bookingManagementService.cancelFutureByResource(resourceId, "Resource unavailable");
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
            throw new IllegalStateException("Failed to process inbound domain event", ex);
        }
    }

    private UUID uuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
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
