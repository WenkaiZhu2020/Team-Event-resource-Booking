package com.teamresource.notification.service.template;

import com.teamresource.notification.infra.messaging.BookingEventPayload;

public abstract class AbstractBookingTemplateRenderer implements NotificationTemplateRenderer {

    @Override
    public RenderedNotification render(BookingEventPayload payload) {
        return new RenderedNotification(type(), buildSubject(payload), buildBody(payload));
    }

    protected String bookingSummary(BookingEventPayload payload) {
        return payload.resourceName() + " on " + payload.startAt();
    }

    protected abstract com.teamresource.notification.domain.NotificationType type();

    protected abstract String buildSubject(BookingEventPayload payload);

    protected abstract String buildBody(BookingEventPayload payload);
}
