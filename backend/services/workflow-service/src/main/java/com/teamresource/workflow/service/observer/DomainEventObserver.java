package com.teamresource.workflow.service.observer;

import com.teamresource.workflow.service.command.CreateApprovalCommand;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface DomainEventObserver {

    boolean supports(String eventType);

    List<CreateApprovalCommand> onEvent(String source, String messageId, JsonNode payload);
}
