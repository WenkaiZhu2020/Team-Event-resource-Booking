package com.teamresource.booking.infra.client;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ResourceClient {

    private final RestClient resourceRestClient;

    public ResourceClient(RestClient resourceRestClient) {
        this.resourceRestClient = resourceRestClient;
    }

    public ResourceSnapshot getResource(UUID resourceId) {
        ApiEnvelope<ResourceSnapshot> envelope = resourceRestClient.get()
                .uri("/api/v1/resources/{resourceId}", resourceId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new ResponseStatusException(response.getStatusCode(), "Failed to load resource details");
                })
                .body(new org.springframework.core.ParameterizedTypeReference<ApiEnvelope<ResourceSnapshot>>() {
                });
        if (envelope == null || envelope.data() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "Resource service returned an empty response");
        }
        return envelope.data();
    }

    public record ResourceSnapshot(
            UUID resourceId,
            UUID managerId,
            String name,
            String description,
            String type,
            String location,
            Integer capacity,
            String status,
            String approvalMode,
            boolean requiresApproval,
            boolean allowWaitlist,
            int maxBookingDurationMinutes,
            int advanceBookingWindowDays,
            List<AvailabilityRuleSnapshot> availabilityRules,
            List<MaintenanceSlotSnapshot> maintenanceSlots,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record AvailabilityRuleSnapshot(
            UUID availabilityRuleId,
            int dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            boolean available
    ) {
    }

    public record MaintenanceSlotSnapshot(
            UUID maintenanceSlotId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String reason,
            String status
    ) {
    }
}
