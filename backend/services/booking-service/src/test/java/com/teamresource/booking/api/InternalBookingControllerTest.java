package com.teamresource.booking.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.service.BookingFacade;
import com.teamresource.booking.service.command.ApplyWorkflowDecisionCommand;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InternalBookingControllerTest {

    private final StubBookingFacade bookingFacade = new StubBookingFacade();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalBookingController(bookingFacade))
                .setControllerAdvice(new ApiGlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

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
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-01T10:00:00Z");
        OffsetDateTime approvedAt = OffsetDateTime.parse("2026-05-01T11:00:00Z");
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
                createdAt,
                approvedAt,
                "valid",
                null,
                createdAt,
                createdAt,
                ApprovalStatus.APPROVED,
                true,
                createdAt,
                approvedAt,
                null,
                null,
                null,
                UUID.randomUUID(),
                approvedAt,
                "corr-1",
                null
        );
    }
}
