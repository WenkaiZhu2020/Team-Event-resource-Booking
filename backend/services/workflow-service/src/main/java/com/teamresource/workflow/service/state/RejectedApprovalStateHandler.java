package com.teamresource.workflow.service.state;

import com.teamresource.workflow.domain.ApprovalDecisionType;
import com.teamresource.workflow.domain.ApprovalStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RejectedApprovalStateHandler implements ApprovalStateHandler {

    @Override
    public ApprovalStatus supportedStatus() {
        return ApprovalStatus.REJECTED;
    }

    @Override
    public ApprovalStatus transition(ApprovalDecisionType decisionType) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Rejected requests cannot be decided again");
    }
}
