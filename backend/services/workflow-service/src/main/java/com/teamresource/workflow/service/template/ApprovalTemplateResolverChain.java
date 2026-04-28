package com.teamresource.workflow.service.template;

import com.teamresource.workflow.service.command.CreateApprovalCommand;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ApprovalTemplateResolverChain {

    private final List<ApprovalTemplateHandler> handlers;

    public ApprovalTemplateResolverChain(List<ApprovalTemplateHandler> handlers) {
        this.handlers = handlers;
    }

    public ApprovalTemplateDefinition resolve(CreateApprovalCommand command) {
        return handlers.stream()
                .filter(handler -> handler.supports(command))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "No approval template is available for this request"
                ))
                .resolve(command);
    }
}
