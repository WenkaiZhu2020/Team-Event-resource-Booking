package com.teamresource.workflow.service.state;

import com.teamresource.workflow.domain.ApprovalDecisionType;
import com.teamresource.workflow.domain.ApprovalStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ApprovedApprovalStateHandler implements ApprovalStateHandler {

    @Override
    public ApprovalStatus supportedStatus() {
        return ApprovalStatus.APPROVED;
    }

    @Override
    public ApprovalStatus transition(ApprovalDecisionType decisionType) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Approved requests cannot be decided again");
    }
}
