package com.teamresource.booking.application.state;

import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.domain.model.BookingStatus;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PendingApprovalStateHandler extends AbstractBookingStateHandler {

    @Override
    public BookingStatus supports() {
        return BookingStatus.PENDING_APPROVAL;
    }

    @Override
    public String cancel(BookingEntity booking, String reason, OffsetDateTime now) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        booking.setCancellationReason(reason);
        return "BOOKING_CANCELLED";
    }

    @Override
    public String approve(BookingEntity booking, UUID approverUserId, OffsetDateTime now) {
        booking.setStatus(BookingStatus.APPROVED);
        booking.setApprovalStatus(ApprovalStatus.APPROVED);
        booking.setApprovedBy(approverUserId);
        booking.setApprovedAt(now);
        booking.setConfirmedAt(now);
        return "BOOKING_APPROVED";
    }

    @Override
    public String reject(BookingEntity booking, UUID approverUserId, String reason, OffsetDateTime now) {
        booking.setStatus(BookingStatus.REJECTED);
        booking.setApprovalStatus(ApprovalStatus.REJECTED);
        booking.setApprovedBy(approverUserId);
        booking.setRejectedAt(now);
        booking.setRejectionReason(reason);
        return "BOOKING_REJECTED";
    }
}
