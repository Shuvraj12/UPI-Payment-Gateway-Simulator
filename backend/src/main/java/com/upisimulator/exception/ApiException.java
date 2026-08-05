package com.upisimulator.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for exceptions that map directly to an HTTP status. Introduced in
 * Phase 2 once a second and third custom exception showed up alongside
 * {@link ResourceNotFoundException} - one {@code @ExceptionHandler(ApiException.class)}
 * in {@link GlobalExceptionHandler} scales far better across 14 phases than
 * a dedicated handler method per exception type.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

}
