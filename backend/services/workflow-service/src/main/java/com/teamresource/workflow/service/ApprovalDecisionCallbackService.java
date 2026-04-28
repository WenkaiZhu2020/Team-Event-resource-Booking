package com.teamresource.workflow.service;

import com.teamresource.workflow.api.dto.ApprovalDecisionCallbackRequest;
import com.teamresource.workflow.domain.ApprovalTargetType;
import com.teamresource.workflow.infra.client.BookingWorkflowClient;
import com.teamresource.workflow.infra.client.EventWorkflowClient;
import com.teamresource.workflow.service.command.CompleteApprovalCallbackCommand;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApprovalDecisionCallbackService {

    private final BookingWorkflowClient bookingWorkflowClient;
    private final EventWorkflowClient eventWorkflowClient;

    public ApprovalDecisionCallbackService(
            BookingWorkflowClient bookingWorkflowClient,
            EventWorkflowClient eventWorkflowClient
    ) {
        this.bookingWorkflowClient = bookingWorkflowClient;
        this.eventWorkflowClient = eventWorkflowClient;
    }

    public void complete(CompleteApprovalCallbackCommand command) {
        if (command.targetType() == ApprovalTargetType.BOOKING) {
            bookingWorkflowClient.applyDecision(
                    command.targetId(),
                    new ApprovalDecisionCallbackRequest(command.decision(), command.note())
            );
            return;
        }
        if (command.targetType() == ApprovalTargetType.EVENT) {
            eventWorkflowClient.applyDecision(
                    command.targetId(),
                    new EventWorkflowClient.ApprovalDecisionPayload(command.decision(), command.note())
            );
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported approval target type for callback completion");
    }
}
