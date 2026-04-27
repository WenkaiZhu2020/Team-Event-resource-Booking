package com.teamresource.notification.service.template;

import com.teamresource.notification.domain.NotificationType;
import com.teamresource.notification.infra.messaging.BookingEventPayload;
import org.springframework.stereotype.Component;

@Component
public class BookingApprovedTemplateRenderer extends AbstractBookingTemplateRenderer {

    @Override
    public boolean supports(String eventType) {
        return "booking.approved".equalsIgnoreCase(eventType);
    }

    @Override
    protected NotificationType type() {
        return NotificationType.BOOKING_APPROVED;
    }

    @Override
    protected String buildSubject(BookingEventPayload payload) {
        return "Booking approved";
    }

    @Override
    protected String buildBody(BookingEventPayload payload) {
        return "Your booking for " + bookingSummary(payload) + " has been approved.";
    }
}
