package com.upisimulator.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested domain resource (user, wallet, transaction, ...)
 * doesn't exist or doesn't belong to the authenticated caller.
 * Caught by {@link GlobalExceptionHandler}'s {@code ApiException} handler
 * and mapped to a 404.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

}
