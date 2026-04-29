package com.teamresource.booking.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.booking.common.error.ApiException;
import com.teamresource.booking.domain.event.DomainEvent;
import com.teamresource.booking.domain.event.DomainEventPublisher;
import com.teamresource.booking.domain.model.OutboxStatus;
import com.teamresource.booking.domain.repository.OutboxRepository;
import com.teamresource.booking.infrastructure.persistence.entity.BookingOutboxEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Profile("stage2-layered-inactive")
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxDomainEventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        BookingOutboxEntity outbox = new BookingOutboxEntity();
        outbox.setId(UUID.randomUUID());
        outbox.setAggregateType(event.aggregateType());
        outbox.setAggregateId(event.aggregateId());
        outbox.setEventType(event.eventType());
        outbox.setPayloadJson(toJson(event.payload()));
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setAttempts(0);
        outbox.setOccurredAt(event.occurredAt());
        outbox.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        outboxRepository.save(outbox);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ApiException("OUTBOX_SERIALIZATION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize booking event payload");
        }
    }
}
