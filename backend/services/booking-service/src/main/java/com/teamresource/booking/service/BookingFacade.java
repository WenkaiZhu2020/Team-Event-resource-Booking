package com.teamresource.booking.service;

import com.teamresource.booking.api.dto.BookingDecisionRequest;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.api.dto.CreateBookingRequest;
import com.teamresource.booking.api.dto.WaitlistEntryResponse;
import com.teamresource.booking.service.command.ApproveBookingCommand;
import com.teamresource.booking.service.command.ApplyWorkflowDecisionCommand;
import com.teamresource.booking.service.command.CancelBookingCommand;
import com.teamresource.booking.service.command.CreateBookingCommand;
import com.teamresource.booking.service.command.RejectBookingCommand;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service("bookingApiFacade")
public class BookingFacade {

    private final BookingService bookingService;

    public BookingFacade(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public BookingResponse create(CreateBookingCommand command) {
        return bookingService.create(command.userId(), command.request(), command.idempotencyKey());
    }

    public List<BookingResponse> myBookings(UUID userId, String status) {
        return bookingService.myBookings(userId, status);
    }

    public BookingResponse byId(UUID bookingId, UUID currentUserId, boolean admin) {
        return bookingService.byId(bookingId, currentUserId, admin);
    }

    public BookingResponse cancel(CancelBookingCommand command) {
        return bookingService.cancel(command.bookingId(), command.currentUserId(), command.admin());
    }

    public List<BookingResponse> pendingApprovals(UUID currentUserId, boolean admin) {
        return bookingService.pendingApprovals(currentUserId, admin);
    }

    public BookingResponse approve(ApproveBookingCommand command) {
        return bookingService.approve(command.bookingId(), command.currentUserId(), command.admin(), command.request());
    }

    public BookingResponse reject(RejectBookingCommand command) {
        return bookingService.reject(command.bookingId(), command.currentUserId(), command.admin(), command.request());
    }

    public BookingResponse applyWorkflowDecision(ApplyWorkflowDecisionCommand command) {
        return bookingService.applyWorkflowDecision(command.bookingId(), command.decision(), command.note());
    }

    public Page<BookingResponse> search(
            UUID userId,
            UUID resourceId,
            String status,
            OffsetDateTime from,
            OffsetDateTime to,
            int page,
            int size
    ) {
        return bookingService.search(
                userId,
                resourceId,
                status,
                from,
                to,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)))
        );
    }

    public List<WaitlistEntryResponse> listWaitlist(UUID resourceId) {
        return bookingService.listWaitlist(resourceId);
    }
}
