package com.teamresource.workflow.service.template;

import com.teamresource.workflow.service.command.CreateApprovalCommand;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Order(100)
public class FallbackTemplateHandler implements ApprovalTemplateHandler {

    @Override
    public boolean supports(CreateApprovalCommand command) {
        return true;
    }

    @Override
    public ApprovalTemplateDefinition resolve(CreateApprovalCommand command) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This version only supports booking approval workflows");
    }
}
