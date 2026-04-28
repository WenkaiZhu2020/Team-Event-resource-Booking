package com.teamresource.booking.application.state;

import com.teamresource.booking.common.error.ApiException;
import com.teamresource.booking.domain.model.BookingStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class BookingStateMachine {

    private final Map<BookingStatus, BookingStateHandler> handlers = new EnumMap<>(BookingStatus.class);

    public BookingStateMachine(List<BookingStateHandler> stateHandlers) {
        for (BookingStateHandler handler : stateHandlers) {
            handlers.put(handler.supports(), handler);
        }
    }

    public BookingStateHandler forStatus(BookingStatus status) {
        BookingStateHandler handler = handlers.get(status);
        if (handler == null) {
            throw new ApiException("BOOKING_STATE_HANDLER_MISSING", HttpStatus.INTERNAL_SERVER_ERROR, "No state handler for " + status);
        }
        return handler;
    }
}
