package com.teamresource.booking.application.facade;

import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.application.command.ApproveBookingCommand;
import com.teamresource.booking.application.command.CancelBookingCommand;
import com.teamresource.booking.application.command.CreateBookingCommand;
import com.teamresource.booking.application.command.RejectBookingCommand;
import com.teamresource.booking.application.service.BookingManagementService;
import com.teamresource.booking.application.service.BookingViewMapper;
import com.teamresource.booking.application.service.CreateBookingCommandHandler;
import com.teamresource.booking.domain.model.BookingSearchCriteria;
import com.teamresource.booking.domain.repository.WaitlistRepository;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import com.teamresource.booking.infrastructure.persistence.entity.WaitlistEntryEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class BookingFacade {

    private final CreateBookingCommandHandler createBookingCommandHandler;
    private final BookingManagementService bookingManagementService;
    private final WaitlistRepository waitlistRepository;
    private final BookingViewMapper mapper;

    public BookingFacade(
            CreateBookingCommandHandler createBookingCommandHandler,
            BookingManagementService bookingManagementService,
            WaitlistRepository waitlistRepository,
            BookingViewMapper mapper
    ) {
        this.createBookingCommandHandler = createBookingCommandHandler;
        this.bookingManagementService = bookingManagementService;
        this.waitlistRepository = waitlistRepository;
        this.mapper = mapper;
    }

    public BookingResponse create(CreateBookingCommand command) {
        BookingEntity booking = createBookingCommandHandler.handle(command);
        return toResponse(booking);
    }

    public BookingResponse cancel(CancelBookingCommand command) {
        BookingEntity booking = bookingManagementService.cancel(command);
        return toResponse(booking);
    }

    public BookingResponse approve(ApproveBookingCommand command) {
        BookingEntity booking = bookingManagementService.approve(command);
        return toResponse(booking);
    }

    public BookingResponse reject(RejectBookingCommand command) {
        BookingEntity booking = bookingManagementService.reject(command);
        return toResponse(booking);
    }

    public BookingResponse byId(UUID bookingId) {
        return toResponse(bookingManagementService.getBooking(bookingId));
    }

    public Page<BookingResponse> list(BookingSearchCriteria criteria, Pageable pageable) {
        return bookingManagementService.search(criteria, pageable).map(this::toResponse);
    }

    public java.util.List<com.teamresource.booking.api.dto.WaitlistEntryResponse> listWaitlist(UUID resourceId) {
        return bookingManagementService.listWaitlist(resourceId).stream()
                .map(w -> new com.teamresource.booking.api.dto.WaitlistEntryResponse(
                        w.getId(),
                        w.getPositionIndex(),
                        w.getStatus(),
                        w.getPromotedAt()
                ))
                .toList();
    }

    private BookingResponse toResponse(BookingEntity booking) {
        WaitlistEntryEntity waitlist = waitlistRepository.findByBookingId(booking.getId()).orElse(null);
        return mapper.toResponse(booking, waitlist);
    }
}
