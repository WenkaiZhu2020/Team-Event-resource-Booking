package com.teamresource.notification.service.template;

import com.teamresource.notification.domain.NotificationType;
import com.teamresource.notification.infra.messaging.BookingEventPayload;
import org.springframework.stereotype.Component;

@Component
public class BookingCreatedTemplateRenderer extends AbstractBookingTemplateRenderer {

    @Override
    public boolean supports(String eventType) {
        return "booking.created".equalsIgnoreCase(eventType);
    }

    @Override
    protected NotificationType type() {
        return NotificationType.BOOKING_CREATED;
    }

    @Override
    protected String buildSubject(BookingEventPayload payload) {
        return "Booking request received";
    }

    @Override
    protected String buildBody(BookingEventPayload payload) {
        return "Your booking request for " + bookingSummary(payload) + " has been recorded with status " + payload.status() + ".";
    }
}
