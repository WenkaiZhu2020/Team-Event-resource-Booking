package com.teamresource.booking.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.service.BookingFacade;
import com.teamresource.booking.service.command.ApplyWorkflowDecisionCommand;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = InternalBookingController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.booking.infra.security.JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.booking.infra.security.InternalApiKeyFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(InternalBookingControllerTest.TestConfig.class)
class InternalBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubBookingFacade bookingFacade;

    @Test
    void applyDecisionShouldReturnWorkflowAppliedBooking() throws Exception {
        UUID bookingId = UUID.randomUUID();
        bookingFacade.response = bookingResponse();

        mockMvc.perform(post("/api/v1/internal/bookings/{bookingId}/decision", bookingId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "decision", "APPROVED",
                                "note", "valid"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void applyDecisionShouldValidateDecisionField() throws Exception {
        UUID bookingId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/internal/bookings/{bookingId}/decision", bookingId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "decision", "",
                                "note", "invalid"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubBookingFacade bookingFacade() {
            return new StubBookingFacade();
        }
    }

    static class StubBookingFacade extends BookingFacade {

        private BookingResponse response;

        StubBookingFacade() {
            super(null);
        }

        @Override
        public BookingResponse applyWorkflowDecision(ApplyWorkflowDecisionCommand command) {
            return response;
        }
    }

    private static BookingResponse bookingResponse() {
        return new BookingResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                "Room A",
                UUID.randomUUID(),
                "ROOM",
                OffsetDateTime.parse("2026-06-01T10:00:00Z"),
                OffsetDateTime.parse("2026-06-01T11:00:00Z"),
                "Weekly sync",
                "APPROVED",
                "MANAGER_APPROVAL",
                null,
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                OffsetDateTime.parse("2026-05-01T11:00:00Z"),
                "valid",
                null,
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                OffsetDateTime.parse("2026-05-01T10:00:00Z")
        );
    }
}
