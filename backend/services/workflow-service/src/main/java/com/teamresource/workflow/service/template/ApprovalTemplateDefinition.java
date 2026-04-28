package com.teamresource.workflow.service.template;

import com.teamresource.workflow.domain.ApprovalScope;

public record ApprovalTemplateDefinition(
        String approvalType,
        String title,
        String summary,
        int totalSteps,
        ApprovalScope approvalScope
) {
}
