package com.teamresource.notification.service.template;

import com.teamresource.notification.domain.NotificationType;
import com.teamresource.notification.infra.messaging.BookingEventPayload;
import org.springframework.stereotype.Component;

@Component
public class BookingCancelledTemplateRenderer extends AbstractBookingTemplateRenderer {

    @Override
    public boolean supports(String eventType) {
        return "booking.cancelled".equalsIgnoreCase(eventType);
    }

    @Override
    protected NotificationType type() {
        return NotificationType.BOOKING_CANCELLED;
    }

    @Override
    protected String buildSubject(BookingEventPayload payload) {
        return "Booking cancelled";
    }

    @Override
    protected String buildBody(BookingEventPayload payload) {
        return "A booking for " + bookingSummary(payload) + " has been cancelled.";
    }
}
