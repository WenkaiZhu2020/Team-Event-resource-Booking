package com.teamresource.workflow.service.assignment;

import com.teamresource.workflow.api.dto.CreateApprovalRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ApproverResolverChain {

    private final List<ApproverResolver> resolvers;

    public ApproverResolverChain(List<ApproverResolver> resolvers) {
        this.resolvers = resolvers;
    }

    public UUID resolve(CreateApprovalRequest request) {
        return resolvers.stream()
                .map(resolver -> resolver.resolve(request))
                .flatMap(java.util.Optional::stream)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No approver could be resolved for this request"));
    }
}
