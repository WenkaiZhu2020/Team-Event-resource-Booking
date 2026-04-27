package com.teamresource.analytics.service;

import com.teamresource.analytics.infra.messaging.DomainEventMessage;
import com.teamresource.analytics.infra.persistence.ConsumedEventEntity;
import com.teamresource.analytics.infra.persistence.ConsumedEventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsEventService {

    private final ConsumedEventRepository consumedEventRepository;
    private final AnalyticsEventDispatcher analyticsEventDispatcher;

    public AnalyticsEventService(
            ConsumedEventRepository consumedEventRepository,
            AnalyticsEventDispatcher analyticsEventDispatcher
    ) {
        this.consumedEventRepository = consumedEventRepository;
        this.analyticsEventDispatcher = analyticsEventDispatcher;
    }

    @Transactional
    public void consume(DomainEventMessage eventMessage) {
        try {
            ConsumedEventEntity consumedEvent = new ConsumedEventEntity();
            consumedEvent.setMessageId(eventMessage.messageId());
            consumedEvent.setAggregateType(eventMessage.aggregateType());
            consumedEvent.setAggregateId(eventMessage.aggregateId());
            consumedEvent.setEventType(eventMessage.eventType());
            consumedEvent.setConsumedAt(OffsetDateTime.now(ZoneOffset.UTC));
            consumedEventRepository.save(consumedEvent);
        } catch (DataIntegrityViolationException ignored) {
            return;
        }

        analyticsEventDispatcher.dispatch(eventMessage);
    }
}
