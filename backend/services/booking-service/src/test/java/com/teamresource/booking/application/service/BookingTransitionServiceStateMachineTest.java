package com.teamresource.booking.application.service;

import com.teamresource.booking.application.state.BookingStateMachine;
import com.teamresource.booking.application.state.BookingStateMachineConfig;
import com.teamresource.booking.common.error.ApiException;
import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.domain.model.BookingStatus;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(classes = {
        BookingStateMachineConfig.class,
        BookingStateMachine.class,
        BookingTransitionService.class
})
class BookingTransitionServiceStateMachineTest {

    @Autowired
    private BookingTransitionService transitionService;

    @Test
    void approveShouldUseSpringStateMachineTransition() {
        BookingEntity booking = booking(BookingStatus.PENDING_APPROVAL, true);
        UUID approverUserId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        String eventType = transitionService.approve(booking, approverUserId, now);

        assertThat(eventType).isEqualTo("BOOKING_APPROVED");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(booking.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(booking.getApprovedBy()).isEqualTo(approverUserId);
        assertThat(booking.getApprovedAt()).isEqualTo(now);
        assertThat(booking.getConfirmedAt()).isEqualTo(now);
    }

    @Test
    void promoteWaitlistedBookingShouldUseStateMachineTransitions() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        BookingEntity approvalRequiredBooking = booking(BookingStatus.WAITLISTED, true);
        BookingEntity autoApprovedBooking = booking(BookingStatus.WAITLISTED, false);

        String approvalEventType = transitionService.promoteToPendingApproval(approvalRequiredBooking, now);
        String confirmedEventType = transitionService.promoteToConfirmed(autoApprovedBooking, now);

        assertThat(approvalEventType).isEqualTo("BOOKING_APPROVAL_REQUESTED");
        assertThat(approvalRequiredBooking.getStatus()).isEqualTo(BookingStatus.PENDING_APPROVAL);
        assertThat(approvalRequiredBooking.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);

        assertThat(confirmedEventType).isEqualTo("BOOKING_APPROVED");
        assertThat(autoApprovedBooking.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(autoApprovedBooking.getApprovalStatus()).isEqualTo(ApprovalStatus.NOT_REQUIRED);
        assertThat(autoApprovedBooking.getConfirmedAt()).isEqualTo(now);
    }

    @Test
    void rejectingConfirmedBookingShouldBeRejectedByStateMachine() {
        BookingEntity booking = booking(BookingStatus.APPROVED, true);

        assertThatThrownBy(() -> transitionService.reject(booking, UUID.randomUUID(), "too late", OffsetDateTime.now(ZoneOffset.UTC)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot reject booking in state APPROVED");
    }

    private BookingEntity booking(BookingStatus status, boolean approvalRequired) {
        BookingEntity booking = new BookingEntity();
        booking.setId(UUID.randomUUID());
        booking.setUserId(UUID.randomUUID());
        booking.setResourceId(UUID.randomUUID());
        booking.setStartAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        booking.setEndAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).plusHours(1));
        booking.setStatus(status);
        booking.setApprovalRequired(approvalRequired);
        booking.setApprovalStatus(approvalRequired ? ApprovalStatus.PENDING : ApprovalStatus.NOT_REQUIRED);
        booking.setRequestedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return booking;
    }
}
