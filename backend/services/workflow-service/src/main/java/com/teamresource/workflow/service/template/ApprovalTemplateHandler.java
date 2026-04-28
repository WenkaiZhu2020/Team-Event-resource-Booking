package com.teamresource.workflow.service.template;

import com.teamresource.workflow.service.command.CreateApprovalCommand;

public interface ApprovalTemplateHandler {

    boolean supports(CreateApprovalCommand command);

    ApprovalTemplateDefinition resolve(CreateApprovalCommand command);
}
