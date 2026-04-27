package com.teamresource.analytics.infra.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DomainEventMessage(
        UUID messageId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        JsonNode payload,
        OffsetDateTime occurredAt
) {
}
