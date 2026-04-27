package com.teamresource.notification.service.template;

import com.teamresource.notification.domain.NotificationType;
import com.teamresource.notification.infra.messaging.BookingEventPayload;
import org.springframework.stereotype.Component;

@Component
public class BookingRejectedTemplateRenderer extends AbstractBookingTemplateRenderer {

    @Override
    public boolean supports(String eventType) {
        return "booking.rejected".equalsIgnoreCase(eventType);
    }

    @Override
    protected NotificationType type() {
        return NotificationType.BOOKING_REJECTED;
    }

    @Override
    protected String buildSubject(BookingEventPayload payload) {
        return "Booking rejected";
    }

    @Override
    protected String buildBody(BookingEventPayload payload) {
        return "Your booking for " + bookingSummary(payload) + " was rejected."
                + (payload.decisionNote() == null ? "" : " Note: " + payload.decisionNote());
    }
}
