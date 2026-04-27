package com.teamresource.event.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.event.api.dto.CreateEventRequest;
import com.teamresource.event.api.dto.EventRegistrationResponse;
import com.teamresource.event.api.dto.EventResponse;
import com.teamresource.event.api.dto.UpdateEventRequest;
import com.teamresource.event.service.EventRegistrationService;
import com.teamresource.event.service.EventService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = EventController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.event.infra.security.JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.event.infra.security.InternalApiKeyFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(EventControllerTest.TestConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubEventService eventService;

    @Autowired
    private StubEventRegistrationService eventRegistrationService;

    @Test
    void publishedEventsShouldReturnCollection() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        eventService.publishedEventsResponse = List.of(new EventResponse(
                eventId,
                organizerId,
                "Architecture Meetup",
                "A technical event",
                "MEETING",
                "Auditorium",
                50,
                12,
                3,
                5,
                OffsetDateTime.parse("2026-05-01T09:00:00Z"),
                OffsetDateTime.parse("2026-05-30T23:00:00Z"),
                OffsetDateTime.parse("2026-06-01T09:00:00Z"),
                OffsetDateTime.parse("2026-06-01T10:00:00Z"),
                "PUBLISHED",
                OffsetDateTime.parse("2026-05-01T09:00:00Z"),
                OffsetDateTime.parse("2026-05-01T09:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.data[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data[0].attendeeProjectedCount").value(12))
                .andExpect(jsonPath("$.data[0].waitlistProjectedCount").value(3))
                .andExpect(jsonPath("$.data[0].checkedInCount").value(5));
    }

    @Test
    void createShouldValidateFutureEventTimes() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .principal(() -> UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "title", "Too Soon",
                                "description", "Invalid event",
                                "category", "MEETING",
                                "location", "Room 5",
                                "capacity", 20,
                                "startAt", "2025-01-01T10:00:00Z",
                                "endAt", "2025-01-01T11:00:00Z"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void publishShouldReturnUpdatedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(organizerId.toString(), null, "ROLE_ORGANIZER");
        eventService.publishResponse = new EventResponse(
                eventId,
                organizerId,
                "Architecture Meetup",
                "A technical event",
                "MEETING",
                "Auditorium",
                50,
                0,
                0,
                0,
                OffsetDateTime.parse("2026-05-01T09:00:00Z"),
                OffsetDateTime.parse("2026-05-30T23:00:00Z"),
                OffsetDateTime.parse("2026-06-01T09:00:00Z"),
                OffsetDateTime.parse("2026-06-01T10:00:00Z"),
                "PUBLISHED",
                OffsetDateTime.parse("2026-05-01T09:00:00Z"),
                OffsetDateTime.parse("2026-05-02T09:00:00Z")
        );

        mockMvc.perform(post("/api/v1/events/{eventId}/publish", eventId)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void registerShouldReturnRegistrationResponse() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        eventRegistrationService.registrationResponse = new EventRegistrationResponse(
                UUID.randomUUID(),
                eventId,
                userId,
                "REGISTERED",
                null,
                OffsetDateTime.parse("2026-05-01T09:00:00Z"),
                null,
                null,
                null,
                OffsetDateTime.parse("2026-05-01T09:00:00Z")
        );

        mockMvc.perform(post("/api/v1/events/{eventId}/registrations", eventId)
                        .principal(userId::toString))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.data.status").value("REGISTERED"));
    }

    @Test
    void checkInShouldReturnUpdatedRegistration() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(organizerId.toString(), null, "ROLE_ORGANIZER");
        eventRegistrationService.checkInResponse = new EventRegistrationResponse(
                registrationId,
                eventId,
                UUID.randomUUID(),
                "REGISTERED",
                null,
                OffsetDateTime.parse("2026-05-01T09:00:00Z"),
                null,
                OffsetDateTime.parse("2026-06-01T09:10:00Z"),
                organizerId,
                OffsetDateTime.parse("2026-06-01T09:10:00Z")
        );

        mockMvc.perform(post("/api/v1/events/{eventId}/registrations/{registrationId}/check-in", eventId, registrationId)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedInBy").value(organizerId.toString()));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubEventService eventService() {
            return new StubEventService();
        }

        @Bean
        StubEventRegistrationService eventRegistrationService() {
            return new StubEventRegistrationService();
        }
    }

    static class StubEventService extends EventService {

        private List<EventResponse> publishedEventsResponse;
        private EventResponse publishResponse;

        StubEventService() {
            super(null);
        }

        @Override
        public List<EventResponse> publishedEvents() {
            return publishedEventsResponse;
        }

        @Override
        public EventResponse publish(UUID eventId, UUID currentUserId, boolean admin) {
            return publishResponse;
        }

        @Override
        public EventResponse create(UUID organizerId, CreateEventRequest request) {
            return null;
        }

        @Override
        public EventResponse update(UUID eventId, UUID currentUserId, boolean admin, UpdateEventRequest request) {
            return null;
        }
    }

    static class StubEventRegistrationService extends EventRegistrationService {

        private EventRegistrationResponse registrationResponse;
        private EventRegistrationResponse checkInResponse;

        StubEventRegistrationService() {
            super(null, null);
        }

        @Override
        public EventRegistrationResponse register(UUID eventId, UUID userId) {
            return registrationResponse;
        }

        @Override
        public EventRegistrationResponse checkIn(UUID eventId, UUID registrationId, UUID currentUserId, boolean admin) {
            return checkInResponse;
        }
    }
}
