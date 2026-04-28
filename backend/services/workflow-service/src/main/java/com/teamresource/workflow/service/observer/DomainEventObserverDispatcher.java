package com.teamresource.workflow.service.observer;

import com.fasterxml.jackson.databind.JsonNode;
import com.teamresource.workflow.service.command.CreateApprovalCommand;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DomainEventObserverDispatcher {

    private final List<DomainEventObserver> observers;

    public DomainEventObserverDispatcher(List<DomainEventObserver> observers) {
        this.observers = observers;
    }

    public List<CreateApprovalCommand> dispatch(String source, String messageId, String eventType, JsonNode payload) {
        List<CreateApprovalCommand> commands = new ArrayList<>();
        for (DomainEventObserver observer : observers) {
            if (observer.supports(eventType)) {
                commands.addAll(observer.onEvent(source, messageId, payload));
            }
        }
        return commands;
    }
}
