package com.teamresource.analytics.infra.messaging;

import com.teamresource.analytics.config.AnalyticsProperties;
import com.teamresource.analytics.service.AnalyticsEventService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsEventConsumer {

    private final AnalyticsEventService analyticsEventService;

    public AnalyticsEventConsumer(AnalyticsEventService analyticsEventService) {
        this.analyticsEventService = analyticsEventService;
    }

    @RabbitListener(queues = "${app.analytics.queue-name:analytics.booking.events}")
    public void onDomainEvent(DomainEventMessage eventMessage) {
        analyticsEventService.consume(eventMessage);
    }
}
