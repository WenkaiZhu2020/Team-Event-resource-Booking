package com.teamresource.booking.domain.event;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public interface DomainEvent {

    UUID aggregateId();

    String aggregateType();

    String eventType();

    OffsetDateTime occurredAt();

    Map<String, Object> payload();
}
