package com.teamresource.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.notification.application.facade.NotificationFacade;
import com.teamresource.notification.application.observer.DomainEventObserverDispatcher;
import com.teamresource.notification.domain.repository.ConsumedMessageRepository;
import com.teamresource.notification.infrastructure.persistence.entity.ConsumedMessageEntity;
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
@Profile("source-architecture")
public class BookingDomainEventConsumer {

    private final ObjectMapper objectMapper;
    private final DomainEventObserverDispatcher observerDispatcher;
    private final NotificationFacade notificationFacade;
    private final ConsumedMessageRepository consumedMessageRepository;

    public BookingDomainEventConsumer(
            ObjectMapper objectMapper,
            DomainEventObserverDispatcher observerDispatcher,
            NotificationFacade notificationFacade,
            ConsumedMessageRepository consumedMessageRepository
    ) {
        this.objectMapper = objectMapper;
        this.observerDispatcher = observerDispatcher;
        this.notificationFacade = notificationFacade;
        this.consumedMessageRepository = consumedMessageRepository;
    }

    @RabbitListener(queues = "${app.messaging.consumers.booking-events-queue}")
    @Transactional
    @SuppressWarnings("unchecked")
    public void onMessage(Message message) {
        String source = "booking-domain";
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
            var commands = observerDispatcher.dispatch(source, messageId, payload);
            for (var command : commands) {
                notificationFacade.dispatch(command);
            }

            ConsumedMessageEntity consumed = new ConsumedMessageEntity();
            consumed.setId(UUID.randomUUID());
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
