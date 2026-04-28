package com.teamresource.notification.application.factory;

import com.teamresource.notification.application.decorator.RetryingChannelSenderDecorator;
import com.teamresource.notification.application.pipeline.ChannelSenderStrategy;
import com.teamresource.notification.common.error.ApiException;
import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.infrastructure.config.MessagingProperties;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ChannelSenderFactory {

    private final Map<NotificationChannel, ChannelSenderStrategy> senders;

    public ChannelSenderFactory(List<ChannelSenderStrategy> strategies, MessagingProperties messagingProperties) {
        Map<NotificationChannel, ChannelSenderStrategy> map = new EnumMap<>(NotificationChannel.class);
        int retries = Math.max(1, messagingProperties.retry().maxAttempts());
        for (ChannelSenderStrategy strategy : strategies) {
            map.put(strategy.channel(), new RetryingChannelSenderDecorator(strategy, retries));
        }
        this.senders = Map.copyOf(map);
    }

    public ChannelSenderStrategy get(NotificationChannel channel) {
        ChannelSenderStrategy sender = senders.get(channel);
        if (sender == null) {
            throw new ApiException(
                    "CHANNEL_SENDER_NOT_FOUND",
                    HttpStatus.BAD_REQUEST,
                    "Unsupported notification channel: " + channel
            );
        }
        return sender;
    }
}
