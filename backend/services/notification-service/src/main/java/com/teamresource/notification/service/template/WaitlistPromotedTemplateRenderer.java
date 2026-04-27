package com.teamresource.notification.service.template;

import com.teamresource.notification.domain.NotificationType;
import com.teamresource.notification.infra.messaging.BookingEventPayload;
import org.springframework.stereotype.Component;

@Component
public class WaitlistPromotedTemplateRenderer extends AbstractBookingTemplateRenderer {

    @Override
    public boolean supports(String eventType) {
        return "booking.waitlist.promoted".equalsIgnoreCase(eventType);
    }

    @Override
    protected NotificationType type() {
        return NotificationType.WAITLIST_PROMOTED;
    }

    @Override
    protected String buildSubject(BookingEventPayload payload) {
        return "Waitlist promoted";
    }

    @Override
    protected String buildBody(BookingEventPayload payload) {
        return "Your waitlisted booking for " + bookingSummary(payload) + " is now active with status " + payload.status() + ".";
    }
}
