package com.teamresource.workflow.service.assignment;

import com.teamresource.workflow.api.dto.CreateApprovalRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ExplicitApproverResolver implements ApproverResolver {

    @Override
    public Optional<UUID> resolve(CreateApprovalRequest request) {
        return Optional.ofNullable(request.approverId());
    }
}
