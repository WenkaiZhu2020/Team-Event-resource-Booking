package com.teamresource.booking.application.service;

import com.teamresource.booking.domain.event.BookingEvent;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class BookingEventFactory {

    public BookingEvent from(String type, BookingEntity booking, String reason, OffsetDateTime occurredAt) {
        return new BookingEvent(
                type,
                booking.getId(),
                booking.getUserId(),
                booking.getResourceId(),
                booking.getEventId(),
                booking.getStatus(),
                booking.getApprovalStatus(),
                booking.getStartAt(),
                booking.getEndAt(),
                reason,
                occurredAt
        );
    }
}
