package com.teamresource.workflow.service.assignment;

import com.teamresource.workflow.api.dto.CreateApprovalRequest;
import java.util.Optional;
import java.util.UUID;

public interface ApproverResolver {

    Optional<UUID> resolve(CreateApprovalRequest request);
}
