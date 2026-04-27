package com.teamresource.notification.infra.client;

import com.teamresource.notification.config.ClientProperties;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EventServiceClient {

    private final RestClient eventRestClient;
    private final ClientProperties properties;

    public EventServiceClient(RestClient eventRestClient, ClientProperties properties) {
        this.eventRestClient = eventRestClient;
        this.properties = properties;
    }

    public List<EventReminderCandidate> fetchDueReminders(OffsetDateTime windowStart, OffsetDateTime windowEnd) {
        ApiEnvelope<List<EventReminderCandidate>> envelope = eventRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/internal/events/reminders/due")
                        .queryParam("windowStart", windowStart)
                        .queryParam("windowEnd", windowEnd)
                        .build())
                .header(properties.internalApiHeaderName(), properties.eventServiceApiKey())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return envelope == null || envelope.data() == null ? List.of() : envelope.data();
    }
}
