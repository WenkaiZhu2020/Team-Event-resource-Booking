package com.teamresource.booking.infra.client;

import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.config.ClientProperties;
import java.time.OffsetDateTime;
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

    public void createBookingApproval(BookingResponse booking) {
        workflowRestClient.post()
                .uri("/api/v1/internal/workflows/approvals")
                .header(properties.internalApiHeaderName(), properties.workflowServiceApiKey())
                .body(new CreateApprovalRequest(
                        "BOOKING",
                        booking.bookingId(),
                        booking.userId(),
                        null,
                        booking.resourceManagerId(),
                        booking.resourceId(),
                        buildTitle(booking),
                        "RESOURCE_BOOKING_APPROVAL",
                        buildSummary(booking),
                        booking.startAt(),
                        booking.endAt(),
                        null
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new ResponseStatusException(response.getStatusCode(), "Workflow service approval creation failed");
                })
                .toBodilessEntity();
    }

    private String buildTitle(BookingResponse booking) {
        return "Booking approval for " + booking.resourceName();
    }

    private String buildSummary(BookingResponse booking) {
        return "Requested slot " + booking.startAt() + " to " + booking.endAt()
                + (booking.purpose() == null ? "" : " for " + booking.purpose());
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
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            java.util.List<UUID> additionalApproverIds
    ) {
    }
}
