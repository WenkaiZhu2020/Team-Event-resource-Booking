package com.teamresource.notification.application.pipeline;

import com.teamresource.notification.domain.model.NotificationChannel;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InAppChannelSender extends AbstractChannelSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    protected String doSend(NotificationDispatchContext context) {
        return "in-app-" + UUID.randomUUID();
    }
}
