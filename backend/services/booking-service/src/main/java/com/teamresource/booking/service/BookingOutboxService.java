package com.teamresource.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.infra.persistence.OutboxMessageEntity;
import com.teamresource.booking.infra.persistence.OutboxMessageRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingOutboxService {

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    public BookingOutboxService(OutboxMessageRepository outboxMessageRepository, ObjectMapper objectMapper) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
    }

    public void record(String eventType, BookingResponse response) {
        record(eventType, response.bookingId(), response);
    }

    public void record(String eventType, UUID aggregateId, Object payload) {
        OutboxMessageEntity message = new OutboxMessageEntity();
        message.setMessageId(UUID.randomUUID());
        message.setAggregateType("BOOKING");
        message.setAggregateId(aggregateId);
        message.setEventType(eventType);
        message.setPayload(toJson(payload));
        message.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        outboxMessageRepository.save(message);
    }

    public DomainEventMessage toDomainEvent(OutboxMessageEntity entity) {
        return new DomainEventMessage(
                entity.getMessageId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                toJsonNode(entity.getPayload()),
                entity.getCreatedAt()
        );
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize outbox payload");
        }
    }

    private JsonNode toJsonNode(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to deserialize outbox payload");
        }
    }

    public record DomainEventMessage(
            UUID messageId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            JsonNode payload,
            OffsetDateTime occurredAt
    ) {
    }
}
