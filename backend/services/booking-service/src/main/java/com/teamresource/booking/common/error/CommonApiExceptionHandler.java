package com.teamresource.booking.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CommonApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::fieldError)
                .toList();

        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                Instant.now(),
                400,
                "VALIDATION_ERROR",
                "Request validation failed",
                req.getRequestURI(),
                fields
        ));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        return ResponseEntity.status(ex.getHttpStatus()).body(new ApiErrorResponse(
                Instant.now(),
                ex.getHttpStatus().value(),
                ex.getCode(),
                ex.getMessage(),
                req.getRequestURI(),
                List.of()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorResponse(
                Instant.now(),
                403,
                "FORBIDDEN",
                ex.getMessage(),
                req.getRequestURI(),
                List.of()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                Instant.now(),
                500,
                "INTERNAL_ERROR",
                "Unexpected server error",
                req.getRequestURI(),
                List.of()
        ));
    }

    private ApiErrorResponse.FieldError fieldError(FieldError err) {
        return new ApiErrorResponse.FieldError(err.getField(), err.getDefaultMessage());
    }
}
