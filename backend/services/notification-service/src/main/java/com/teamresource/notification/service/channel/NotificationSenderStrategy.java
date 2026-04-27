package com.teamresource.notification.service.channel;

import com.teamresource.notification.domain.NotificationChannel;
import com.teamresource.notification.infra.persistence.NotificationRecordEntity;

public interface NotificationSenderStrategy {

    NotificationChannel channel();

    void send(NotificationRecordEntity notificationRecord);
}
