package com.upisimulator.exception;

/**
 * Thrown when a requested domain resource (user, wallet, transaction, ...)
 * doesn't exist or doesn't belong to the authenticated caller.
 * Caught by {@link GlobalExceptionHandler} and mapped to a 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

}
