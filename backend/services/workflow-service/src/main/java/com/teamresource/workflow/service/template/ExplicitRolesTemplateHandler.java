package com.teamresource.workflow.service.template;

import com.teamresource.workflow.domain.ApprovalScope;
import com.teamresource.workflow.service.command.CreateApprovalCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class ExplicitRolesTemplateHandler implements ApprovalTemplateHandler {

    @Override
    public boolean supports(CreateApprovalCommand command) {
        return command.additionalApproverIds() != null && !command.additionalApproverIds().isEmpty();
    }

    @Override
    public ApprovalTemplateDefinition resolve(CreateApprovalCommand command) {
        String approvalType = command.approvalType() == null || command.approvalType().isBlank()
                ? "MULTI_STEP_APPROVAL"
                : command.approvalType().trim();
        String title = command.title() == null || command.title().isBlank()
                ? "Multi-step approval request"
                : command.title().trim();
        String summary = command.summary() == null || command.summary().isBlank()
                ? null
                : command.summary().trim();
        return new ApprovalTemplateDefinition(
                approvalType,
                title,
                summary,
                1 + command.additionalApproverIds().size(),
                ApprovalScope.ASSIGNED_USER
        );
    }
}
