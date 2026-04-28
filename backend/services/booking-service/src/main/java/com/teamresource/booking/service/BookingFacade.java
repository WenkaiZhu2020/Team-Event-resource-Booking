package com.teamresource.booking.service;

import com.teamresource.booking.api.dto.BookingDecisionRequest;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.api.dto.CreateBookingRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BookingFacade {

    private final BookingService bookingService;

    public BookingFacade(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public BookingResponse create(UUID userId, CreateBookingRequest request, String idempotencyKey) {
        return bookingService.create(userId, request, idempotencyKey);
    }

    public List<BookingResponse> myBookings(UUID userId, String status) {
        return bookingService.myBookings(userId, status);
    }

    public BookingResponse byId(UUID bookingId, UUID currentUserId, boolean admin) {
        return bookingService.byId(bookingId, currentUserId, admin);
    }

    public BookingResponse cancel(UUID bookingId, UUID currentUserId, boolean admin) {
        return bookingService.cancel(bookingId, currentUserId, admin);
    }

    public List<BookingResponse> pendingApprovals(UUID currentUserId, boolean admin) {
        return bookingService.pendingApprovals(currentUserId, admin);
    }

    public BookingResponse approve(UUID bookingId, UUID currentUserId, boolean admin, BookingDecisionRequest request) {
        return bookingService.approve(bookingId, currentUserId, admin, request);
    }

    public BookingResponse reject(UUID bookingId, UUID currentUserId, boolean admin, BookingDecisionRequest request) {
        return bookingService.reject(bookingId, currentUserId, admin, request);
    }

    public BookingResponse applyWorkflowDecision(UUID bookingId, String decision, String note) {
        return bookingService.applyWorkflowDecision(bookingId, decision, note);
    }
}
