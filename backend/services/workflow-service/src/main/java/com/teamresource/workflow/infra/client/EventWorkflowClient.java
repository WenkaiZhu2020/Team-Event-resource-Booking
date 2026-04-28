package com.teamresource.workflow.infra.client;

import com.teamresource.workflow.config.ClientProperties;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class EventWorkflowClient {

    private final RestClient eventRestClient;
    private final ClientProperties properties;

    public EventWorkflowClient(RestClient eventRestClient, ClientProperties properties) {
        this.eventRestClient = eventRestClient;
        this.properties = properties;
    }

    public void applyDecision(UUID eventId, ApprovalDecisionPayload request) {
        eventRestClient.post()
                .uri("/api/v1/internal/events/{eventId}/decision", eventId)
                .header(properties.internalApiHeaderName(), properties.eventServiceApiKey())
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ResponseStatusException(res.getStatusCode(), "Event service decision callback failed");
                })
                .toBodilessEntity();
    }

    public record ApprovalDecisionPayload(
            String decision,
            String note
    ) {
    }
}
