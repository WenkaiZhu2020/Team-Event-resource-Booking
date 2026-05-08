package com.teamresource.booking.application.state;

import com.teamresource.booking.domain.model.ApprovalStatus;
import com.teamresource.booking.domain.model.BookingStatus;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

@Configuration
@EnableStateMachineFactory
public class BookingStateMachineConfig extends EnumStateMachineConfigurerAdapter<BookingStateMachineState, BookingStateMachineEvent> {

    static final String BOOKING_KEY = "booking";
    static final String NOW_KEY = "now";
    static final String REASON_KEY = "reason";
    static final String APPROVER_USER_ID_KEY = "approverUserId";

    @Override
    public void configure(StateMachineConfigurationConfigurer<BookingStateMachineState, BookingStateMachineEvent> config)
            throws Exception {
        config.withConfiguration().autoStartup(false);
    }

    @Override
    public void configure(StateMachineStateConfigurer<BookingStateMachineState, BookingStateMachineEvent> states)
            throws Exception {
        states.withStates()
                .initial(BookingStateMachineState.PENDING)
                .states(java.util.EnumSet.allOf(BookingStateMachineState.class))
                .end(BookingStateMachineState.CONFIRMED)
                .end(BookingStateMachineState.REJECTED)
                .end(BookingStateMachineState.CANCELLED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<BookingStateMachineState, BookingStateMachineEvent> transitions)
            throws Exception {
        transitions
                .withExternal()
                    .source(BookingStateMachineState.PENDING)
                    .target(BookingStateMachineState.CONFIRMED)
                    .event(BookingStateMachineEvent.APPROVE)
                    .action(approveAction())
                .and()
                .withExternal()
                    .source(BookingStateMachineState.PENDING)
                    .target(BookingStateMachineState.REJECTED)
                    .event(BookingStateMachineEvent.REJECT)
                    .action(rejectAction())
                .and()
                .withExternal()
                    .source(BookingStateMachineState.PENDING)
                    .target(BookingStateMachineState.CANCELLED)
                    .event(BookingStateMachineEvent.CANCEL)
                    .action(cancelAction())
                .and()
                .withExternal()
                    .source(BookingStateMachineState.CONFIRMED)
                    .target(BookingStateMachineState.CANCELLED)
                    .event(BookingStateMachineEvent.CANCEL)
                    .action(cancelAction())
                .and()
                .withExternal()
                    .source(BookingStateMachineState.WAITLISTED)
                    .target(BookingStateMachineState.CANCELLED)
                    .event(BookingStateMachineEvent.CANCEL)
                    .action(cancelAction())
                .and()
                .withExternal()
                    .source(BookingStateMachineState.WAITLISTED)
                    .target(BookingStateMachineState.PENDING)
                    .event(BookingStateMachineEvent.PROMOTE_TO_PENDING)
                    .action(promoteToPendingAction())
                .and()
                .withExternal()
                    .source(BookingStateMachineState.WAITLISTED)
                    .target(BookingStateMachineState.CONFIRMED)
                    .event(BookingStateMachineEvent.PROMOTE_TO_CONFIRMED)
                    .action(promoteToConfirmedAction());
    }

    @Bean
    Action<BookingStateMachineState, BookingStateMachineEvent> cancelAction() {
        return context -> {
            BookingEntity booking = booking(context);
            OffsetDateTime now = now(context);
            String reason = (String) context.getExtendedState().getVariables().get(REASON_KEY);
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(now);
            booking.setCancellationReason(reason);
        };
    }

    @Bean
    Action<BookingStateMachineState, BookingStateMachineEvent> approveAction() {
        return context -> {
            BookingEntity booking = booking(context);
            OffsetDateTime now = now(context);
            UUID approverUserId = approverUserId(context);
            booking.setStatus(BookingStatus.APPROVED);
            booking.setApprovalStatus(ApprovalStatus.APPROVED);
            booking.setApprovedBy(approverUserId);
            booking.setApprovedAt(now);
            booking.setConfirmedAt(now);
        };
    }

    @Bean
    Action<BookingStateMachineState, BookingStateMachineEvent> rejectAction() {
        return context -> {
            BookingEntity booking = booking(context);
            OffsetDateTime now = now(context);
            UUID approverUserId = approverUserId(context);
            String reason = (String) context.getExtendedState().getVariables().get(REASON_KEY);
            booking.setStatus(BookingStatus.REJECTED);
            booking.setApprovalStatus(ApprovalStatus.REJECTED);
            booking.setApprovedBy(approverUserId);
            booking.setRejectedAt(now);
            booking.setRejectionReason(reason);
        };
    }

    @Bean
    Action<BookingStateMachineState, BookingStateMachineEvent> promoteToPendingAction() {
        return context -> {
            BookingEntity booking = booking(context);
            booking.setStatus(BookingStatus.PENDING_APPROVAL);
            booking.setApprovalStatus(ApprovalStatus.PENDING);
        };
    }

    @Bean
    Action<BookingStateMachineState, BookingStateMachineEvent> promoteToConfirmedAction() {
        return context -> {
            BookingEntity booking = booking(context);
            OffsetDateTime now = now(context);
            booking.setStatus(BookingStatus.APPROVED);
            booking.setApprovalStatus(ApprovalStatus.NOT_REQUIRED);
            booking.setConfirmedAt(now);
        };
    }

    private BookingEntity booking(org.springframework.statemachine.StateContext<BookingStateMachineState, BookingStateMachineEvent> context) {
        return (BookingEntity) context.getExtendedState().getVariables().get(BOOKING_KEY);
    }

    private OffsetDateTime now(org.springframework.statemachine.StateContext<BookingStateMachineState, BookingStateMachineEvent> context) {
        return (OffsetDateTime) context.getExtendedState().getVariables().get(NOW_KEY);
    }

    private UUID approverUserId(org.springframework.statemachine.StateContext<BookingStateMachineState, BookingStateMachineEvent> context) {
        return (UUID) context.getExtendedState().getVariables().get(APPROVER_USER_ID_KEY);
    }
}
