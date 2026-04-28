package com.teamresource.booking.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.booking.api.dto.BookingDecisionRequest;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.api.dto.CreateBookingRequest;
import com.teamresource.booking.service.BookingFacade;
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
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BookingController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.booking.infra.security.JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.booking.infra.security.InternalApiKeyFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(BookingControllerTest.TestConfig.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubBookingFacade bookingFacade;

    @Test
    void createShouldReturnPayloadAndForwardIdempotencyKey() throws Exception {
        UUID userId = UUID.randomUUID();
        bookingFacade.createResponse = bookingResponse();

        mockMvc.perform(post("/api/v1/bookings")
                        .principal(() -> userId.toString())
                        .header("Idempotency-Key", "idem-123")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                UUID.randomUUID(),
                                null,
                                OffsetDateTime.parse("2026-06-01T10:00:00Z"),
                                OffsetDateTime.parse("2026-06-01T11:00:00Z"),
                                "Weekly sync"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));

        org.assertj.core.api.Assertions.assertThat(bookingFacade.lastIdempotencyKey).isEqualTo("idem-123");
    }

    @Test
    void myBookingsShouldReturnList() throws Exception {
        UUID userId = UUID.randomUUID();
        bookingFacade.myBookingsResponse = List.of(bookingResponse());

        mockMvc.perform(get("/api/v1/bookings/me").principal(() -> userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].resourceName").value("Room A"));
    }

    @Test
    void approveShouldUseAdminRoleFromAuthentication() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        bookingFacade.approvalResponse = bookingResponse();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null, "ROLE_ADMIN");

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/approve", bookingId)
                        .principal(authentication)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new BookingDecisionRequest("approved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));

        org.assertj.core.api.Assertions.assertThat(bookingFacade.lastApproveAdmin).isTrue();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubBookingFacade bookingFacade() {
            return new StubBookingFacade();
        }
    }

    static class StubBookingFacade extends BookingFacade {

        private BookingResponse createResponse;
        private List<BookingResponse> myBookingsResponse = List.of();
        private BookingResponse approvalResponse;
        private String lastIdempotencyKey;
        private boolean lastApproveAdmin;

        StubBookingFacade() {
            super(null);
        }

        @Override
        public BookingResponse create(UUID userId, CreateBookingRequest request, String idempotencyKey) {
            this.lastIdempotencyKey = idempotencyKey;
            return createResponse;
        }

        @Override
        public List<BookingResponse> myBookings(UUID userId, String status) {
            return myBookingsResponse;
        }

        @Override
        public BookingResponse approve(UUID bookingId, UUID currentUserId, boolean admin, BookingDecisionRequest request) {
            this.lastApproveAdmin = admin;
            return approvalResponse;
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
                "PENDING_APPROVAL",
                "MANAGER_APPROVAL",
                null,
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                null,
                null,
                null,
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                OffsetDateTime.parse("2026-05-01T10:00:00Z")
        );
    }
}
