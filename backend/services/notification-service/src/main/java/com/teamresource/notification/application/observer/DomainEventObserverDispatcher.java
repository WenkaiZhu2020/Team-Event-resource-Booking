package com.teamresource.notification.application.observer;

import com.teamresource.notification.application.pipeline.NotificationDispatchCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DomainEventObserverDispatcher {

    private final List<DomainEventObserver> observers;

    public DomainEventObserverDispatcher(List<DomainEventObserver> observers) {
        this.observers = observers;
    }

    public List<NotificationDispatchCommand> dispatch(String source, String messageId, Map<String, Object> payload) {
        String eventType = string(payload.get("eventType"));
        List<NotificationDispatchCommand> commands = new ArrayList<>();
        for (DomainEventObserver observer : observers) {
            if (observer.supports(eventType)) {
                commands.addAll(observer.onEvent(source, messageId, payload));
            }
        }
        return commands;
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }
}
