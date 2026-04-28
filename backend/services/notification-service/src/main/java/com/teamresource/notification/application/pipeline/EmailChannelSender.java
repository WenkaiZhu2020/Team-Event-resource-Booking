package com.teamresource.notification.application.pipeline;

import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.infrastructure.config.ChannelProperties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailChannelSender extends AbstractChannelSender {

    private static final Logger log = LoggerFactory.getLogger(EmailChannelSender.class);

    private final ChannelProperties channelProperties;

    public EmailChannelSender(ChannelProperties channelProperties) {
        this.channelProperties = channelProperties;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    protected String doSend(NotificationDispatchContext context) {
        if (!channelProperties.email().enabled()) {
            throw new IllegalStateException("Email channel disabled");
        }

        log.info("Simulated email send from={} toUser={} notificationId={} title={}",
                channelProperties.email().from(),
                context.notification().getUserId(),
                context.notification().getId(),
                context.notification().getTitle());

        return "email-" + UUID.randomUUID();
    }
}
