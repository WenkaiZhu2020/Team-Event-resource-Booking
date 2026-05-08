package com.teamresource.booking.application.state;

public enum BookingStateMachineEvent {
    APPROVE,
    REJECT,
    CANCEL,
    PROMOTE_TO_PENDING,
    PROMOTE_TO_CONFIRMED
}
