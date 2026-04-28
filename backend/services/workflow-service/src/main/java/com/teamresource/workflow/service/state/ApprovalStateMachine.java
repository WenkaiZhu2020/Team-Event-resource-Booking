package com.teamresource.workflow.service.state;

import com.teamresource.workflow.domain.ApprovalDecisionType;
import com.teamresource.workflow.domain.ApprovalStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ApprovalStateMachine {

    private final Map<ApprovalStatus, ApprovalStateHandler> handlers;

    public ApprovalStateMachine(List<ApprovalStateHandler> handlers) {
        this.handlers = new EnumMap<>(ApprovalStatus.class);
        handlers.forEach(handler -> this.handlers.put(handler.supportedStatus(), handler));
    }

    public ApprovalStatus transition(ApprovalStatus currentStatus, ApprovalDecisionType decisionType) {
        ApprovalStateHandler handler = handlers.get(currentStatus);
        if (handler == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No approval state handler registered");
        }
        return handler.transition(decisionType);
    }
}
