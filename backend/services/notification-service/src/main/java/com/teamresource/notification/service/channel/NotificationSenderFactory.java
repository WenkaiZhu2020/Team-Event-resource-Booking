package com.teamresource.notification.service.channel;

import com.teamresource.notification.domain.NotificationChannel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationSenderFactory {

    private final Map<NotificationChannel, NotificationSenderStrategy> strategies = new EnumMap<>(NotificationChannel.class);

    public NotificationSenderFactory(List<NotificationSenderStrategy> senderStrategies) {
        senderStrategies.forEach(strategy -> strategies.put(strategy.channel(), strategy));
    }

    public NotificationSenderStrategy get(NotificationChannel channel) {
        return strategies.get(channel);
    }
}
