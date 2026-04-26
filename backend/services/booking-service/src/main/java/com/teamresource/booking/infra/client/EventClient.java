package com.teamresource.booking.infra.client;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class EventClient {

    private final RestClient eventRestClient;

    public EventClient(RestClient eventRestClient) {
        this.eventRestClient = eventRestClient;
    }

    public EventSnapshot getEvent(UUID eventId) {
        ApiEnvelope<EventSnapshot> envelope = eventRestClient.get()
                .uri("/api/v1/events/{eventId}", eventId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new ResponseStatusException(response.getStatusCode(), "Failed to load event details");
                })
                .body(new org.springframework.core.ParameterizedTypeReference<ApiEnvelope<EventSnapshot>>() {
                });
        if (envelope == null || envelope.data() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "Event service returned an empty response");
        }
        return envelope.data();
    }

    public record EventSnapshot(
            UUID eventId,
            UUID organizerId,
            String title,
            String description,
            String category,
            String location,
            int capacity,
            OffsetDateTime registrationOpenAt,
            OffsetDateTime registrationCloseAt,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }
}
