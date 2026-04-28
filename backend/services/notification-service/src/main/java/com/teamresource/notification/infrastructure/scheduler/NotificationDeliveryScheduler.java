package com.teamresource.notification.infrastructure.scheduler;

import com.teamresource.notification.application.facade.NotificationFacade;
import com.teamresource.notification.infrastructure.config.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationDeliveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryScheduler.class);

    private final NotificationFacade notificationFacade;
    private final MessagingProperties messagingProperties;

    public NotificationDeliveryScheduler(
            NotificationFacade notificationFacade,
            MessagingProperties messagingProperties
    ) {
        this.notificationFacade = notificationFacade;
        this.messagingProperties = messagingProperties;
    }

    @Scheduled(fixedDelayString = "${app.messaging.retry.backoff-seconds:15}000")
    public void retryFailed() {
        int processed = notificationFacade.retryFailed(100);
        if (processed > 0) {
            log.info("Retried {} failed notifications", processed);
        }
    }

    @Scheduled(fixedDelayString = "${app.messaging.reminder.polling-seconds:60}000")
    public void sendDueReminders() {
        int processed = notificationFacade.sendDueReminders(100);
        if (processed > 0) {
            log.info("Delivered {} scheduled reminder notifications", processed);
        }
    }
}
