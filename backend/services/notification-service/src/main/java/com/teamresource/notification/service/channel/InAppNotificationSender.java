package com.teamresource.notification.service.channel;

import com.teamresource.notification.domain.NotificationChannel;
import com.teamresource.notification.domain.NotificationStatus;
import com.teamresource.notification.infra.persistence.NotificationRecordEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationSender implements NotificationSenderStrategy {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(NotificationRecordEntity notificationRecord) {
        notificationRecord.setStatus(NotificationStatus.SENT);
        notificationRecord.setSentAt(OffsetDateTime.now(ZoneOffset.UTC));
        notificationRecord.setUpdatedAt(notificationRecord.getSentAt());
    }
}
