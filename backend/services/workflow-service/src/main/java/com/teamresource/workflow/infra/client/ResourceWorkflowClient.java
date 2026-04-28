package com.teamresource.workflow.infra.client;

import com.teamresource.workflow.config.ClientProperties;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ResourceWorkflowClient implements ResourceApprovalPolicyGateway {

    private final RestClient resourceRestClient;
    private final ClientProperties properties;

    public ResourceWorkflowClient(RestClient resourceRestClient, ClientProperties properties) {
        this.resourceRestClient = resourceRestClient;
        this.properties = properties;
    }

    @Override
    public ResourceApprovalPolicy resolve(UUID resourceId) {
        if (resourceId == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Resource id is required for booking approval policy");
        }
        ApiEnvelope<ResourceApprovalPolicyResponse> response = resourceRestClient.get()
                .uri("/api/v1/internal/resources/{resourceId}/approval-policy", resourceId)
                .header(properties.internalApiHeaderName(), properties.resourceServiceApiKey())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ResponseStatusException(res.getStatusCode(), "Resource service approval policy lookup failed");
                })
                .body(new org.springframework.core.ParameterizedTypeReference<>() {
                });
        if (response == null || response.data() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Resource service returned an empty approval policy response");
        }
        ResourceApprovalPolicyResponse data = response.data();
        return new ResourceApprovalPolicy(data.resourceId(), data.managerId(), data.approvalMode(), data.requiresApproval());
    }

    public record ResourceApprovalPolicyResponse(
            UUID resourceId,
            UUID managerId,
            String approvalMode,
            boolean requiresApproval,
            int maxBookingDurationMinutes,
            int advanceBookingWindowDays,
            boolean allowWaitlist
    ) {
    }
}
