package com.teamresource.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        OutboxMessageEntity message = new OutboxMessageEntity();
        message.setMessageId(UUID.randomUUID());
        message.setAggregateType("BOOKING");
        message.setAggregateId(response.bookingId());
        message.setEventType(eventType);
        message.setPayload(toJson(response));
        message.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        outboxMessageRepository.save(message);
    }

    private String toJson(BookingResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize outbox payload");
        }
    }
}
