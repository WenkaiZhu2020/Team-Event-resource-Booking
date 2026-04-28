package com.teamresource.workflow.service.template;

import com.teamresource.workflow.domain.ApprovalTargetType;
import com.teamresource.workflow.domain.ApprovalScope;
import com.teamresource.workflow.service.command.CreateApprovalCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class EventApprovalTemplateHandler implements ApprovalTemplateHandler {

    @Override
    public boolean supports(CreateApprovalCommand command) {
        return command.targetType() == ApprovalTargetType.EVENT;
    }

    @Override
    public ApprovalTemplateDefinition resolve(CreateApprovalCommand command) {
        String approvalType = command.approvalType() == null || command.approvalType().isBlank()
                ? "EVENT_APPROVAL"
                : command.approvalType().trim();
        String title = command.title() == null || command.title().isBlank()
                ? "Event approval request"
                : command.title().trim();
        String summary = command.summary() == null || command.summary().isBlank()
                ? null
                : command.summary().trim();
        return new ApprovalTemplateDefinition(approvalType, title, summary, 1, ApprovalScope.ADMIN_ONLY);
    }
}
