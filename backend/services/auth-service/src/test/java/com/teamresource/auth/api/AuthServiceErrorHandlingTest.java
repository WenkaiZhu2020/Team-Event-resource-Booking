package com.teamresource.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

class AuthServiceErrorHandlingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleStatusException() {
        var response = handler.handleStatus(new ResponseStatusException(HttpStatus.CONFLICT, "duplicate"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("duplicate");
    }

    @Test
    void shouldHandleStatusExceptionWithoutExplicitReason() {
        var response = handler.handleStatus(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Not Found");
        assertThat(response.getBody().error()).isEqualTo("Not Found");
    }

    @Test
    void shouldHandleValidationException() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must be a well-formed email address"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        var response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().details()).containsExactly("email: must be a well-formed email address");
    }

    @Test
    void shouldHandleUnexpectedException() {
        var response = handler.handleUnhandled(new IllegalStateException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Unexpected server error");
        assertThat(response.getBody().details()).isEqualTo(List.of());
        assertThat(response.getBody().timestamp()).isBeforeOrEqualTo(OffsetDateTime.now());
    }
}
