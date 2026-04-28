package com.teamresource.booking.api.controller;

import com.teamresource.booking.api.dto.ApiResponse;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.api.dto.InternalWorkflowApproveRequest;
import com.teamresource.booking.api.dto.InternalWorkflowRejectRequest;
import com.teamresource.booking.application.command.ApproveBookingCommand;
import com.teamresource.booking.application.command.RejectBookingCommand;
import com.teamresource.booking.application.facade.BookingFacade;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/bookings")
@Profile("source-architecture")
public class BookingInternalController {

    private final BookingFacade bookingFacade;

    public BookingInternalController(BookingFacade bookingFacade) {
        this.bookingFacade = bookingFacade;
    }

    @PostMapping("/{bookingId}/workflow-approve")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public ApiResponse<BookingResponse> approve(
            @PathVariable UUID bookingId,
            @Valid @RequestBody InternalWorkflowApproveRequest request
    ) {
        return ApiResponse.of(bookingFacade.approve(new ApproveBookingCommand(bookingId, request.approverUserId())));
    }

    @PostMapping("/{bookingId}/workflow-reject")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public ApiResponse<BookingResponse> reject(
            @PathVariable UUID bookingId,
            @Valid @RequestBody InternalWorkflowRejectRequest request
    ) {
        return ApiResponse.of(bookingFacade.reject(new RejectBookingCommand(bookingId, request.approverUserId(), request.reason())));
    }
}
