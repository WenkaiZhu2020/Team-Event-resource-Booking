package com.teamresource.booking.application.state;

import com.teamresource.booking.common.error.ApiException;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import com.teamresource.booking.domain.model.BookingStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import reactor.core.publisher.Mono;

@Component
public class BookingStateMachine {

    private final StateMachineFactory<BookingStateMachineState, BookingStateMachineEvent> stateMachineFactory;

    public BookingStateMachine(StateMachineFactory<BookingStateMachineState, BookingStateMachineEvent> stateMachineFactory) {
        this.stateMachineFactory = stateMachineFactory;
    }

    public String cancel(BookingEntity booking, String reason, OffsetDateTime now) {
        BookingStatus previousStatus = booking.getStatus();
        sendEvent(booking, BookingStateMachineEvent.CANCEL, now, null, reason);
        return previousStatus == BookingStatus.WAITLISTED ? "BOOKING_WAITLIST_CANCELLED" : "BOOKING_CANCELLED";
    }

    public String approve(BookingEntity booking, UUID approverUserId, OffsetDateTime now) {
        sendEvent(booking, BookingStateMachineEvent.APPROVE, now, approverUserId, null);
        return "BOOKING_APPROVED";
    }

    public String reject(BookingEntity booking, UUID approverUserId, String reason, OffsetDateTime now) {
        sendEvent(booking, BookingStateMachineEvent.REJECT, now, approverUserId, reason);
        return "BOOKING_REJECTED";
    }

    public String promoteToPendingApproval(BookingEntity booking, OffsetDateTime now) {
        sendEvent(booking, BookingStateMachineEvent.PROMOTE_TO_PENDING, now, null, null);
        return "BOOKING_APPROVAL_REQUESTED";
    }

    public String promoteToConfirmed(BookingEntity booking, OffsetDateTime now) {
        sendEvent(booking, BookingStateMachineEvent.PROMOTE_TO_CONFIRMED, now, null, null);
        return "BOOKING_APPROVED";
    }

    private void sendEvent(
            BookingEntity booking,
            BookingStateMachineEvent event,
            OffsetDateTime now,
            UUID approverUserId,
            String reason
    ) {
        StateMachine<BookingStateMachineState, BookingStateMachineEvent> machine =
                stateMachineFactory.getStateMachine(booking.getId().toString());

        machine.stopReactively().block();
        StateMachineContext<BookingStateMachineState, BookingStateMachineEvent> context =
                new DefaultStateMachineContext<>(map(booking.getStatus()), null, null, null);
        machine.getStateMachineAccessor().doWithAllRegions(access -> access.resetStateMachineReactively(context).block());
        machine.getExtendedState().getVariables().clear();
        machine.getExtendedState().getVariables().put(BookingStateMachineConfig.BOOKING_KEY, booking);
        machine.getExtendedState().getVariables().put(BookingStateMachineConfig.NOW_KEY, now);
        if (reason != null) {
            machine.getExtendedState().getVariables().put(BookingStateMachineConfig.REASON_KEY, reason);
        }
        if (approverUserId != null) {
            machine.getExtendedState().getVariables().put(BookingStateMachineConfig.APPROVER_USER_ID_KEY, approverUserId);
        }
        machine.startReactively().block();

        Message<BookingStateMachineEvent> message = MessageBuilder.withPayload(event).build();
        List<StateMachineEventResult<BookingStateMachineState, BookingStateMachineEvent>> results =
                machine.sendEventCollect(Mono.just(message)).block();
        boolean accepted = results != null
                && results.stream().anyMatch(result -> result.getResultType() == StateMachineEventResult.ResultType.ACCEPTED);
        if (!accepted) {
            throw new ApiException(
                    "BOOKING_STATE_TRANSITION_NOT_ALLOWED",
                    HttpStatus.CONFLICT,
                    "Cannot " + event.name().toLowerCase() + " booking in state " + booking.getStatus()
            );
        }
        machine.stopReactively().block();
    }

    private BookingStateMachineState map(BookingStatus status) {
        return switch (status) {
            case PENDING_APPROVAL -> BookingStateMachineState.PENDING;
            case APPROVED -> BookingStateMachineState.CONFIRMED;
            case REJECTED -> BookingStateMachineState.REJECTED;
            case CANCELLED -> BookingStateMachineState.CANCELLED;
            case WAITLISTED -> BookingStateMachineState.WAITLISTED;
        };
    }
}
