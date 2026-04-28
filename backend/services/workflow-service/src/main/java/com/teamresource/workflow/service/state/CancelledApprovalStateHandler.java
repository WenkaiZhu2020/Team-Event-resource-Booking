package com.teamresource.workflow.service.state;

import com.teamresource.workflow.domain.ApprovalDecisionType;
import com.teamresource.workflow.domain.ApprovalStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CancelledApprovalStateHandler implements ApprovalStateHandler {

    @Override
    public ApprovalStatus supportedStatus() {
        return ApprovalStatus.CANCELLED;
    }

    @Override
    public ApprovalStatus transition(ApprovalDecisionType decisionType) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled requests cannot be decided again");
    }
}
