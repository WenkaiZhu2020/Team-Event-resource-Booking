package com.teamresource.workflow.service.observer;

import com.fasterxml.jackson.databind.JsonNode;
import com.teamresource.workflow.domain.ApprovalTargetType;
import com.teamresource.workflow.service.command.CreateApprovalCommand;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BookingApprovalRequestObserver implements DomainEventObserver {

    private static final Set<String> SUPPORTED = Set.of("booking.created", "booking.waitlist.promoted");

    @Override
    public boolean supports(String eventType) {
        return eventType != null && SUPPORTED.contains(eventType);
    }

    @Override
    public List<CreateApprovalCommand> onEvent(String source, String messageId, JsonNode payload) {
        if (!"PENDING_APPROVAL".equals(payload.path("status").asText())) {
            return List.of();
        }
        UUID bookingId = uuid(payload.path("bookingId").asText(null));
        UUID requesterId = uuid(payload.path("userId").asText(null));
        UUID targetOwnerId = uuid(payload.path("resourceManagerId").asText(null));
        UUID resourceId = uuid(payload.path("resourceId").asText(null));
        if (bookingId == null || requesterId == null || targetOwnerId == null || resourceId == null) {
            return List.of();
        }
        String resourceName = payload.path("resourceName").asText("resource");
        String purpose = payload.path("purpose").isNull() ? null : payload.path("purpose").asText(null);
        OffsetDateTime startAt = parseTime(payload.path("startAt").asText(null));
        OffsetDateTime endAt = parseTime(payload.path("endAt").asText(null));
        return List.of(new CreateApprovalCommand(
                ApprovalTargetType.BOOKING,
                bookingId,
                requesterId,
                null,
                targetOwnerId,
                resourceId,
                "Booking approval for " + resourceName,
                "RESOURCE_BOOKING_APPROVAL",
                buildSummary(startAt, endAt, purpose),
                startAt,
                endAt,
                null
        ));
    }

    private String buildSummary(OffsetDateTime startAt, OffsetDateTime endAt, String purpose) {
        String summary = "Requested slot " + startAt + " to " + endAt;
        if (purpose != null && !purpose.isBlank()) {
            return summary + " for " + purpose.trim();
        }
        return summary;
    }

    private UUID uuid(String raw) {
        try {
            return raw == null ? null : UUID.fromString(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private OffsetDateTime parseTime(String raw) {
        try {
            return raw == null ? null : OffsetDateTime.parse(raw);
        } catch (Exception ex) {
            return null;
        }
    }
}
