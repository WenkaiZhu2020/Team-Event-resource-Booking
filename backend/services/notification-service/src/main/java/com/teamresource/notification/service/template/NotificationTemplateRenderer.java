package com.teamresource.notification.service.template;

import com.teamresource.notification.infra.messaging.BookingEventPayload;

public interface NotificationTemplateRenderer {

    boolean supports(String eventType);

    RenderedNotification render(BookingEventPayload payload);
}
