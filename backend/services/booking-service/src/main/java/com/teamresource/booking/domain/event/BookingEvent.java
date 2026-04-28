package com.teamresource.booking.domain.event;

import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.domain.model.BookingStatus;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record BookingEvent(
        String type,
        UUID bookingId,
        UUID userId,
        UUID resourceId,
        UUID eventId,
        BookingStatus bookingStatus,
        ApprovalStatus approvalStatus,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String reason,
        OffsetDateTime occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return bookingId;
    }

    @Override
    public String aggregateType() {
        return "BOOKING";
    }

    @Override
    public String eventType() {
        return type;
    }

    @Override
    public Map<String, Object> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", type);
        payload.put("bookingId", bookingId);
        payload.put("userId", userId);
        payload.put("resourceId", resourceId);
        payload.put("eventId", eventId);
        payload.put("bookingStatus", bookingStatus == null ? null : bookingStatus.name());
        payload.put("approvalStatus", approvalStatus == null ? null : approvalStatus.name());
        payload.put("startAt", startAt);
        payload.put("endAt", endAt);
        payload.put("reason", reason);
        payload.put("occurredAt", occurredAt);
        return payload;
    }
}
