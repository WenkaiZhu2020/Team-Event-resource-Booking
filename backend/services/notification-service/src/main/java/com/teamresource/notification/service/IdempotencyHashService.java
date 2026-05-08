package com.teamresource.notification.service;

import com.teamresource.notification.infra.messaging.DomainEventMessage;
import com.teamresource.notification.infra.persistence.IdempotencyRecordEntity;
import com.teamresource.notification.infra.persistence.IdempotencyRecordRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyHashService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public IdempotencyHashService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Transactional
    public boolean shouldProcess(DomainEventMessage eventMessage) {
        if (eventMessage.aggregateId() == null) {
            return true;
        }
        OffsetDateTime eventTimestamp = eventMessage.occurredAt() == null
                ? OffsetDateTime.now(ZoneOffset.UTC)
                : eventMessage.occurredAt();
        return shouldProcess(eventMessage.aggregateId(), eventMessage.messageId(), eventMessage.eventType(), eventTimestamp);
    }

    private boolean shouldProcess(UUID aggregateId, UUID messageId, String eventType, OffsetDateTime eventTimestamp) {
        IdempotencyRecordEntity record = idempotencyRecordRepository.findByAggregateId(aggregateId).orElse(null);
        if (record == null) {
            try {
                idempotencyRecordRepository.save(newRecord(aggregateId, messageId, eventType, eventTimestamp));
                return true;
            } catch (DataIntegrityViolationException ignored) {
                record = idempotencyRecordRepository.findByAggregateId(aggregateId).orElse(null);
                if (record == null) {
                    return false;
                }
            }
        }

        if (messageId.equals(record.getLastProcessedMessageId())) {
            return false;
        }
        if (record.getLastProcessedEventAt() != null && record.getLastProcessedEventAt().isAfter(eventTimestamp)) {
            return false;
        }

        record.setLastProcessedMessageId(messageId);
        record.setLastProcessedEventType(eventType);
        record.setLastProcessedEventAt(eventTimestamp);
        record.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        idempotencyRecordRepository.save(record);
        return true;
    }

    private IdempotencyRecordEntity newRecord(
            UUID aggregateId,
            UUID messageId,
            String eventType,
            OffsetDateTime eventTimestamp
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setIdempotencyRecordId(UUID.randomUUID());
        record.setAggregateId(aggregateId);
        record.setLastProcessedMessageId(messageId);
        record.setLastProcessedEventType(eventType);
        record.setLastProcessedEventAt(eventTimestamp);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }
}
