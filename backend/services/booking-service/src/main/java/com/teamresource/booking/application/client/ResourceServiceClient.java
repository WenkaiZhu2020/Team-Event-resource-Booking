package com.teamresource.booking.application.client;

import com.teamresource.booking.common.error.ApiException;
import com.teamresource.booking.infrastructure.config.IntegrationProperties;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ResourceServiceClient implements ResourcePrecheckGateway {

    private final RestTemplate restTemplate;
    private final IntegrationProperties properties;

    public ResourceServiceClient(RestTemplateBuilder restTemplateBuilder, IntegrationProperties properties) {
        this.restTemplate = restTemplateBuilder.build();
        this.properties = properties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResourcePrecheckResult precheck(UUID resourceId, OffsetDateTime startAt, OffsetDateTime endAt) {
        String url = properties.resourceServiceBaseUrl() + "/api/v1/internal/resources/" + resourceId + "/booking-precheck";

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Internal-Api-Key", properties.resourceServiceApiKey());

        Map<String, Object> body = Map.of(
                "startsAt", startAt.toString(),
                "endsAt", endAt.toString()
        );

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        } catch (Exception ex) {
            throw new ApiException("RESOURCE_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Cannot validate resource booking availability");
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new ApiException("RESOURCE_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Resource booking precheck failed");
        }

        Object dataRaw = response.getBody().get("data");
        if (!(dataRaw instanceof Map<?, ?> data)) {
            throw new ApiException("RESOURCE_SERVICE_UNEXPECTED_RESPONSE", HttpStatus.SERVICE_UNAVAILABLE, "Unexpected resource precheck response");
        }

        return new ResourcePrecheckResult(
                asBoolean(data.get("bookingAllowed")),
                asString(data.get("reasonCode")),
                asBoolean(data.get("requiresApproval")),
                asBoolean(data.get("allowWaitlist")),
                asInteger(data.get("maxBookingDurationMinutes")),
                asInteger(data.get("bufferBeforeMinutes")),
                asInteger(data.get("bufferAfterMinutes"))
        );
    }

    private boolean asBoolean(Object value) {
        return value instanceof Boolean b ? b : false;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return null;
    }
}
