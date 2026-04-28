package com.teamresource.workflow.service.template;

import com.teamresource.workflow.domain.ApprovalTargetType;
import com.teamresource.workflow.domain.ApprovalScope;
import com.teamresource.workflow.infra.client.ResourceApprovalPolicyGateway;
import com.teamresource.workflow.service.command.CreateApprovalCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class BookingApprovalTemplateHandler implements ApprovalTemplateHandler {

    private final ResourceApprovalPolicyGateway resourceApprovalPolicyGateway;

    public BookingApprovalTemplateHandler(ResourceApprovalPolicyGateway resourceApprovalPolicyGateway) {
        this.resourceApprovalPolicyGateway = resourceApprovalPolicyGateway;
    }

    @Override
    public boolean supports(CreateApprovalCommand command) {
        return command.targetType() == ApprovalTargetType.BOOKING;
    }

    @Override
    public ApprovalTemplateDefinition resolve(CreateApprovalCommand command) {
        var policy = resourceApprovalPolicyGateway.resolve(command.resourceId());
        if (!policy.requiresApproval()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "The resource no longer requires approval"
            );
        }
        boolean adminOnly = "ADMIN_APPROVAL".equals(policy.approvalMode());
        String approvalType = command.approvalType() == null || command.approvalType().isBlank()
                ? (adminOnly ? "RESOURCE_BOOKING_ADMIN_APPROVAL" : "RESOURCE_BOOKING_APPROVAL")
                : command.approvalType().trim();
        String title = command.title() == null || command.title().isBlank()
                ? (adminOnly ? "Admin approval required for resource booking" : "Booking approval request")
                : command.title().trim();
        String summary = command.summary() == null || command.summary().isBlank()
                ? null
                : command.summary().trim();
        return new ApprovalTemplateDefinition(
                approvalType,
                title,
                summary,
                1,
                adminOnly ? ApprovalScope.ADMIN_ONLY : ApprovalScope.ASSIGNED_USER
        );
    }
}
