package com.teamresource.analytics.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.teamresource.analytics.domain.BookingAnalyticsStatus;
import com.teamresource.analytics.infra.messaging.DomainEventMessage;
import com.teamresource.analytics.infra.persistence.BookingFactEntity;
import com.teamresource.analytics.infra.persistence.BookingFactRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BookingEventAggregationStrategy implements AnalyticsAggregationStrategy {

    private static final Set<String> SUPPORTED_PREFIXES = Set.of("booking.");

    private final BookingFactRepository bookingFactRepository;

    public BookingEventAggregationStrategy(BookingFactRepository bookingFactRepository) {
        this.bookingFactRepository = bookingFactRepository;
    }

    @Override
    public boolean supports(DomainEventMessage eventMessage) {
        return eventMessage.eventType() != null && SUPPORTED_PREFIXES.stream().anyMatch(prefix -> eventMessage.eventType().startsWith(prefix));
    }

    @Override
    public void apply(DomainEventMessage eventMessage) {
        JsonNode payload = eventMessage.payload();
        UUID bookingId = uuid(payload.get("bookingId"));
        if (bookingId == null) {
            return;
        }

        BookingFactEntity entity = bookingFactRepository.findById(bookingId).orElseGet(() -> {
            BookingFactEntity created = new BookingFactEntity();
            created.setBookingId(bookingId);
            return created;
        });

        entity.setUserId(uuid(payload.get("userId")));
        entity.setLinkedEventId(uuid(payload.get("linkedEventId")));
        entity.setResourceId(uuid(payload.get("resourceId")));
        entity.setResourceName(text(payload.get("resourceName"), "Unknown resource"));
        entity.setResourceType(text(payload.get("resourceType"), "UNKNOWN"));
        entity.setBookingStatus(parseStatus(text(payload.get("status"), "UNKNOWN")));
        entity.setApprovalMode(text(payload.get("approvalMode"), "UNKNOWN"));
        entity.setWaitlistPosition(integer(payload.get("waitlistPosition")));
        entity.setStartAt(time(payload.get("startAt")));
        entity.setEndAt(time(payload.get("endAt")));
        entity.setCreatedAt(time(payload.get("createdAt")) == null ? now(eventMessage.occurredAt()) : time(payload.get("createdAt")));
        entity.setUpdatedAt(time(payload.get("updatedAt")) == null ? now(eventMessage.occurredAt()) : time(payload.get("updatedAt")));
        entity.setLastEventType(eventMessage.eventType());
        entity.setLastEventAt(now(eventMessage.occurredAt()));

        bookingFactRepository.save(entity);
    }

    private BookingAnalyticsStatus parseStatus(String raw) {
        try {
            return BookingAnalyticsStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return BookingAnalyticsStatus.UNKNOWN;
        }
    }

    private UUID uuid(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return UUID.fromString(node.asText());
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer integer(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asInt();
    }

    private OffsetDateTime time(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(node.asText());
        } catch (Exception ex) {
            return null;
        }
    }

    private String text(JsonNode node, String fallback) {
        return node == null || node.isNull() ? fallback : node.asText(fallback);
    }

    private OffsetDateTime now(OffsetDateTime occurredAt) {
        return occurredAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : occurredAt;
    }
}
