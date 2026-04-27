package com.teamresource.workflow.infra.client;

import com.teamresource.workflow.api.dto.ApprovalDecisionCallbackRequest;
import com.teamresource.workflow.config.ClientProperties;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BookingWorkflowClient {

    private final RestClient bookingRestClient;
    private final ClientProperties properties;

    public BookingWorkflowClient(RestClient bookingRestClient, ClientProperties properties) {
        this.bookingRestClient = bookingRestClient;
        this.properties = properties;
    }

    public void applyDecision(UUID bookingId, ApprovalDecisionCallbackRequest request) {
        bookingRestClient.post()
                .uri("/api/v1/internal/bookings/{bookingId}/decision", bookingId)
                .header(properties.internalApiHeaderName(), properties.bookingServiceApiKey())
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ResponseStatusException(res.getStatusCode(), "Booking service decision callback failed");
                })
                .toBodilessEntity();
    }
}
