package com.teamresource.notification.infra.messaging;

import com.teamresource.notification.config.RabbitConfig;
import com.teamresource.notification.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BookingNotificationConsumer {

    private final NotificationService notificationService;

    public BookingNotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitConfig.BOOKING_EVENTS_QUEUE)
    public void onBookingEvent(DomainEventMessage domainEventMessage) {
        notificationService.consume(domainEventMessage);
    }
}
