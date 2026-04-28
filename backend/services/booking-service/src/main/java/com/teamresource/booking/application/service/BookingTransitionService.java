package com.teamresource.booking.application.service;

import com.teamresource.booking.application.state.BookingStateMachine;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BookingTransitionService {

    private final BookingStateMachine stateMachine;

    public BookingTransitionService(BookingStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    public String cancel(BookingEntity booking, String reason, OffsetDateTime now) {
        return stateMachine.forStatus(booking.getStatus()).cancel(booking, reason, now);
    }

    public String approve(BookingEntity booking, UUID approverUserId, OffsetDateTime now) {
        return stateMachine.forStatus(booking.getStatus()).approve(booking, approverUserId, now);
    }

    public String reject(BookingEntity booking, UUID approverUserId, String reason, OffsetDateTime now) {
        return stateMachine.forStatus(booking.getStatus()).reject(booking, approverUserId, reason, now);
    }
}
