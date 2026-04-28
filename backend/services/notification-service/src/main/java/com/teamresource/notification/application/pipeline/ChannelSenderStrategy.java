package com.teamresource.notification.application.pipeline;

import com.teamresource.notification.domain.model.NotificationChannel;

public interface ChannelSenderStrategy {

    NotificationChannel channel();

    ChannelSendResult send(NotificationDispatchContext context);
}
