package com.teamresource.booking.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.teamresource.booking.api.dto.BookingDecisionRequest;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.api.dto.CreateBookingRequest;
import com.teamresource.booking.api.dto.PageResponse;
import com.teamresource.booking.api.dto.WaitlistEntryResponse;
import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.domain.model.WaitlistStatus;
import com.teamresource.booking.service.BookingFacade;
import com.teamresource.booking.service.command.ApproveBookingCommand;
import com.teamresource.booking.service.command.ApplyWorkflowDecisionCommand;
import com.teamresource.booking.service.command.CancelBookingCommand;
import com.teamresource.booking.service.command.CreateBookingCommand;
import com.teamresource.booking.service.command.RejectBookingCommand;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BookingControllerTest {

    private final StubBookingFacade bookingFacade = new StubBookingFacade();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BookingController(bookingFacade))
                .setControllerAdvice(new ApiGlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createShouldReturnPayloadAndForwardIdempotencyKey() throws Exception {
        UUID userId = UUID.randomUUID();
        bookingFacade.createResponse = bookingResponse("PENDING_APPROVAL");

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
        bookingFacade.myBookingsResponse = List.of(bookingResponse("APPROVED"));

        mockMvc.perform(get("/api/v1/bookings/me").principal(() -> userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].resourceName").value("Room A"));
    }

    @Test
    void approveShouldUseAdminRoleFromAuthentication() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        bookingFacade.approvalResponse = bookingResponse("APPROVED");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null, "ROLE_ADMIN");

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/approve", bookingId)
                        .principal(authentication)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new BookingDecisionRequest("approved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        org.assertj.core.api.Assertions.assertThat(bookingFacade.lastApproveAdmin).isTrue();
    }

    @Test
    void searchShouldRequireManagerOrAdminRole() throws Exception {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(UUID.randomUUID().toString(), null, "ROLE_USER");

        mockMvc.perform(get("/api/v1/bookings").principal(authentication))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Booking access denied"));
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
        public BookingResponse create(CreateBookingCommand command) {
            this.lastIdempotencyKey = command.idempotencyKey();
            return createResponse;
        }

        @Override
        public List<BookingResponse> myBookings(UUID userId, String status) {
            return myBookingsResponse;
        }

        @Override
        public BookingResponse cancel(CancelBookingCommand command) {
            return createResponse;
        }

        @Override
        public BookingResponse approve(ApproveBookingCommand command) {
            this.lastApproveAdmin = command.admin();
            return approvalResponse;
        }

        @Override
        public BookingResponse reject(RejectBookingCommand command) {
            return approvalResponse;
        }

        @Override
        public org.springframework.data.domain.Page<BookingResponse> search(
                UUID userId,
                UUID resourceId,
                String status,
                OffsetDateTime from,
                OffsetDateTime to,
                int page,
                int size
        ) {
            return new PageImpl<>(List.of(bookingResponse("APPROVED")), PageRequest.of(page, size), 1);
        }

        @Override
        public List<WaitlistEntryResponse> listWaitlist(UUID resourceId) {
            return List.of(new WaitlistEntryResponse(UUID.randomUUID(), 1L, WaitlistStatus.WAITING, null));
        }

        @Override
        public BookingResponse applyWorkflowDecision(ApplyWorkflowDecisionCommand command) {
            return approvalResponse;
        }
    }

    private static BookingResponse bookingResponse(String status) {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-01T10:00:00Z");
        OffsetDateTime decidedAt = status.equals("APPROVED") ? OffsetDateTime.parse("2026-05-01T11:00:00Z") : null;
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
                status,
                "MANAGER_APPROVAL",
                null,
                createdAt,
                decidedAt,
                status.equals("APPROVED") ? "approved" : null,
                null,
                createdAt,
                createdAt,
                status.equals("APPROVED") ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING,
                true,
                createdAt,
                decidedAt,
                null,
                null,
                null,
                status.equals("APPROVED") ? UUID.randomUUID() : null,
                decidedAt,
                "corr-1",
                null
        );
    }
}
