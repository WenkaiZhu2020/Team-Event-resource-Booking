package com.teamresource.event.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.event.api.dto.EventReminderCandidateResponse;
import com.teamresource.event.api.dto.EventResponse;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = InternalEventController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.event.infra.security.JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.event.infra.security.InternalApiKeyFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(InternalEventControllerTest.TestConfig.class)
class InternalEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubEventRegistrationService eventRegistrationService;

    @Autowired
    private StubEventService eventService;

    @Test
    void dueRemindersShouldReturnCandidates() throws Exception {
        eventRegistrationService.reminders = List.of(new EventReminderCandidateResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Data Lab Session",
                "Lab A",
                OffsetDateTime.parse("2026-06-01T09:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/internal/events/reminders/due")
                        .param("windowStart", "2026-06-01T07:00:00Z")
                        .param("windowEnd", "2026-06-01T09:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventTitle").value("Data Lab Session"));
    }

    @Test
    void applyApprovalDecisionShouldReturnUpdatedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        eventService.decisionResponse = new EventResponse(
                eventId,
                UUID.randomUUID(),
                "Flagship Conference",
                null,
                "CONFERENCE",
                "Main Hall",
                180,
                0,
                0,
                0,
                null,
                null,
                OffsetDateTime.parse("2026-06-01T09:00:00Z"),
                OffsetDateTime.parse("2026-06-01T12:00:00Z"),
                "PUBLISHED",
                OffsetDateTime.parse("2026-05-01T09:00:00Z"),
                OffsetDateTime.parse("2026-05-02T09:00:00Z")
        );

        mockMvc.perform(post("/api/v1/internal/events/{eventId}/decision", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "decision", "APPROVED",
                                "note", "ok"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubEventRegistrationService eventRegistrationService() {
            return new StubEventRegistrationService();
        }

        @Bean
        StubEventService eventService() {
            return new StubEventService();
        }
    }

    static class StubEventRegistrationService extends EventRegistrationService {
        private List<EventReminderCandidateResponse> reminders = List.of();

        StubEventRegistrationService() {
            super(null, null);
        }

        @Override
        public List<EventReminderCandidateResponse> dueReminders(OffsetDateTime windowStart, OffsetDateTime windowEnd) {
            return reminders;
        }
    }

    static class StubEventService extends EventService {
        private EventResponse decisionResponse;

        StubEventService() {
            super(null);
        }

        @Override
        public EventResponse applyApprovalDecision(UUID eventId, com.teamresource.event.api.dto.InternalApprovalDecisionRequest request) {
            return decisionResponse;
        }
    }
}
