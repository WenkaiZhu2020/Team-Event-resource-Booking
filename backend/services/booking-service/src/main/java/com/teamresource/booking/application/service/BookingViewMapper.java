package com.teamresource.booking.application.service;

import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.api.dto.WaitlistEntryResponse;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import com.teamresource.booking.infrastructure.persistence.entity.WaitlistEntryEntity;
import org.springframework.stereotype.Component;

@Component
public class BookingViewMapper {

    public BookingResponse toResponse(BookingEntity booking, WaitlistEntryEntity waitlist) {
        WaitlistEntryResponse waitlistResponse = waitlist == null ? null : new WaitlistEntryResponse(
                waitlist.getId(),
                waitlist.getPositionIndex(),
                waitlist.getStatus(),
                waitlist.getPromotedAt()
        );

        return new BookingResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getEventId(),
                booking.getResourceId(),
                booking.getStartAt(),
                booking.getEndAt(),
                booking.getStatus(),
                booking.getApprovalStatus(),
                booking.isApprovalRequired(),
                booking.getRequestedAt(),
                booking.getConfirmedAt(),
                booking.getCancelledAt(),
                booking.getCancellationReason(),
                booking.getRejectedAt(),
                booking.getRejectionReason(),
                booking.getApprovedBy(),
                booking.getApprovedAt(),
                booking.getCorrelationId(),
                waitlistResponse
        );
    }
}
