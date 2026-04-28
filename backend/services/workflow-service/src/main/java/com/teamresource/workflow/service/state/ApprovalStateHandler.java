package com.teamresource.workflow.service.state;

import com.teamresource.workflow.domain.ApprovalDecisionType;
import com.teamresource.workflow.domain.ApprovalStatus;

public interface ApprovalStateHandler {

    ApprovalStatus supportedStatus();

    ApprovalStatus transition(ApprovalDecisionType decisionType);
}
