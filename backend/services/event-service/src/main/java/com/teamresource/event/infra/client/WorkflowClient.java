package com.teamresource.event.infra.client;

import com.teamresource.event.api.dto.EventResponse;
import com.teamresource.event.config.ClientProperties;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class WorkflowClient {

    private final RestClient workflowRestClient;
    private final ClientProperties properties;

    public WorkflowClient(RestClient workflowRestClient, ClientProperties properties) {
        this.workflowRestClient = workflowRestClient;
        this.properties = properties;
    }

    public void createEventApproval(EventResponse event) {
        workflowRestClient.post()
                .uri("/api/v1/internal/workflows/approvals")
                .header(properties.internalApiHeaderName(), properties.workflowServiceApiKey())
                .body(new CreateApprovalRequest(
                        "EVENT",
                        event.eventId(),
                        event.organizerId(),
                        null,
                        event.organizerId(),
                        null,
                        "Event publication approval for " + event.title(),
                        "EVENT_PUBLICATION_APPROVAL",
                        buildSummary(event),
                        null,
                        null,
                        null
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new ResponseStatusException(response.getStatusCode(), "Workflow service event approval creation failed");
                })
                .toBodilessEntity();
    }

    private String buildSummary(EventResponse event) {
        return "Publish event " + event.title() + " at " + event.startAt() + " with capacity " + event.capacity();
    }

    public record CreateApprovalRequest(
            String targetType,
            UUID targetId,
            UUID requesterId,
            UUID approverId,
            UUID targetOwnerId,
            UUID resourceId,
            String title,
            String approvalType,
            String summary,
            java.time.OffsetDateTime startAt,
            java.time.OffsetDateTime endAt,
            java.util.List<UUID> additionalApproverIds
    ) {
    }
}
