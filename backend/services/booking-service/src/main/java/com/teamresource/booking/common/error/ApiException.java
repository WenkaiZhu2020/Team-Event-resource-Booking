package com.teamresource.booking.common.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public ApiException(String code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
