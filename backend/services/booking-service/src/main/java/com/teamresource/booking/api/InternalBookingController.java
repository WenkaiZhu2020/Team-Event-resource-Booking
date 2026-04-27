package com.teamresource.booking.api;

import com.teamresource.booking.api.dto.ApiResponse;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.api.dto.InternalBookingDecisionRequest;
import com.teamresource.booking.service.BookingService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/bookings")
public class InternalBookingController {

    private final BookingService bookingService;

    public InternalBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/{bookingId}/decision")
    public ApiResponse<BookingResponse> applyDecision(
            @PathVariable UUID bookingId,
            @Valid @RequestBody InternalBookingDecisionRequest request
    ) {
        return ApiResponse.of(bookingService.applyWorkflowDecision(bookingId, request.decision(), request.note()));
    }
}
