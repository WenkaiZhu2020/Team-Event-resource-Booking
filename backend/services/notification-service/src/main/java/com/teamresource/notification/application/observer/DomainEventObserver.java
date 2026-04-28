package com.teamresource.notification.application.observer;

import com.teamresource.notification.application.pipeline.NotificationDispatchCommand;
import java.util.List;
import java.util.Map;

public interface DomainEventObserver {

    boolean supports(String eventType);

    List<NotificationDispatchCommand> onEvent(String source, String messageId, Map<String, Object> payload);
}
