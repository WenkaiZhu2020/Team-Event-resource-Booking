package com.teamresource.notification.application.observer;

import com.teamresource.notification.application.pipeline.NotificationDispatchCommand;
import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.domain.model.NotificationType;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BookingDomainEventObserver implements DomainEventObserver {

    private static final Set<String> SUPPORTED = Set.of(
            "BOOKING_CONFIRMED",
            "BOOKING_REJECTED",
            "BOOKING_APPROVED",
            "BOOKING_WAITLIST_PROMOTED",
            "BOOKING_CANCELLED",
            "BOOKING_CANCELLED_BY_EVENT",
            "BOOKING_CANCELLED_BY_RESOURCE"
    );

    @Override
    public boolean supports(String eventType) {
        return eventType != null && SUPPORTED.contains(eventType);
    }

    @Override
    public List<NotificationDispatchCommand> onEvent(String source, String messageId, Map<String, Object> payload) {
        String eventType = string(payload.get("eventType"));
        UUID userId = uuid(payload.get("userId"));
        UUID bookingId = uuid(payload.get("bookingId"));
        if (userId == null || bookingId == null) {
            return List.of();
        }

        Map<String, Object> templateData = new LinkedHashMap<>();
        templateData.put("bookingId", string(payload.get("bookingId")));
        templateData.put("resourceId", string(payload.get("resourceId")));
        templateData.put("eventId", string(payload.get("eventId")));
        templateData.put("startAt", string(payload.get("startAt")));
        templateData.put("endAt", string(payload.get("endAt")));
        templateData.put("reason", string(payload.get("reason")));

        String idempotencyBase = source + ":" + eventType + ":" + bookingId + ":" + messageId;

        if ("BOOKING_CONFIRMED".equals(eventType)) {
            NotificationDispatchCommand immediate = new NotificationDispatchCommand(
                    userId,
                    NotificationType.BOOKING_CONFIRMED,
                    "BOOKING_CONFIRMED",
                    source,
                    eventType,
                    "BOOKING",
                    bookingId,
                    idempotencyBase,
                    templateData,
                    List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    null,
                    null
            );

            OffsetDateTime reminderAt = parseTime(payload.get("startAt"));
            reminderAt = reminderAt == null ? null : reminderAt.minusMinutes(60);

            NotificationDispatchCommand reminder = new NotificationDispatchCommand(
                    userId,
                    NotificationType.EVENT_REMINDER,
                    "EVENT_REMINDER",
                    source,
                    "EVENT_REMINDER",
                    "BOOKING",
                    bookingId,
                    idempotencyBase + ":REMINDER",
                    templateData,
                    List.of(NotificationChannel.IN_APP),
                    reminderAt,
                    3
            );
            return List.of(immediate, reminder);
        }

        if ("BOOKING_REJECTED".equals(eventType)) {
            return List.of(new NotificationDispatchCommand(
                    userId,
                    NotificationType.BOOKING_REJECTED,
                    "BOOKING_REJECTED",
                    source,
                    eventType,
                    "BOOKING",
                    bookingId,
                    idempotencyBase,
                    templateData,
                    List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    null,
                    null
            ));
        }

        if ("BOOKING_APPROVED".equals(eventType)) {
            return List.of(new NotificationDispatchCommand(
                    userId,
                    NotificationType.BOOKING_APPROVED,
                    "BOOKING_APPROVED",
                    source,
                    eventType,
                    "BOOKING",
                    bookingId,
                    idempotencyBase,
                    templateData,
                    List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    null,
                    null
            ));
        }

        if ("BOOKING_WAITLIST_PROMOTED".equals(eventType)) {
            return List.of(new NotificationDispatchCommand(
                    userId,
                    NotificationType.WAITLIST_PROMOTED,
                    "WAITLIST_PROMOTED",
                    source,
                    eventType,
                    "BOOKING",
                    bookingId,
                    idempotencyBase,
                    templateData,
                    List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                    null,
                    null
            ));
        }

        return List.of(new NotificationDispatchCommand(
                userId,
                NotificationType.BOOKING_CANCELLED,
                "BOOKING_REJECTED",
                source,
                eventType,
                "BOOKING",
                bookingId,
                idempotencyBase,
                templateData,
                List.of(NotificationChannel.IN_APP),
                null,
                null
        ));
    }

    private UUID uuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private OffsetDateTime parseTime(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }
}
