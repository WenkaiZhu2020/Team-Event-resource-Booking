package com.teamresource.notification.service.channel;

import com.teamresource.notification.domain.NotificationChannel;
import com.teamresource.notification.domain.NotificationStatus;
import com.teamresource.notification.infra.persistence.NotificationRecordEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailSimulationSender implements NotificationSenderStrategy {

    private static final Logger log = LoggerFactory.getLogger(EmailSimulationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationRecordEntity notificationRecord) {
        log.info("Simulated email notification to user {}: {}", notificationRecord.getUserId(), notificationRecord.getSubject());
        notificationRecord.setStatus(NotificationStatus.SENT);
        notificationRecord.setSentAt(OffsetDateTime.now(ZoneOffset.UTC));
        notificationRecord.setUpdatedAt(notificationRecord.getSentAt());
    }
}
