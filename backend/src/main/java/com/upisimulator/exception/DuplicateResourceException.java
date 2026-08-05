package com.upisimulator.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when registering with an email or phone number that's already in
 * use. Mapped to 409 Conflict.
 */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

}
