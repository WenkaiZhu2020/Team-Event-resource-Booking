package com.teamresource.user.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

class UserServiceErrorHandlingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleResponseStatusException() {
        var response = handler.handleStatus(new ResponseStatusException(HttpStatus.NOT_FOUND, "missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("missing");
    }

    @Test
    void shouldFallbackToReasonPhraseWhenReasonMissing() {
        var response = handler.handleStatus(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().message()).isEqualTo("Unauthorized");
        assertThat(response.getBody().error()).isEqualTo("Unauthorized");
    }

    @Test
    void shouldHandleValidationException() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "displayName", "must not be blank"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        var response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().details()).containsExactly("displayName: must not be blank");
    }

    @Test
    void shouldHandleUnexpectedException() {
        var response = handler.handleUnhandled(new IllegalStateException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().details()).isEqualTo(List.of());
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(OffsetDateTime.now());
    }
}
