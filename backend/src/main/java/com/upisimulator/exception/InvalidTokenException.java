package com.upisimulator.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a refresh token is malformed, expired, not recognized, or has
 * already been revoked (used once already, or logged out). Mapped to 401 -
 * the client's move either way is the same: send the user back to /login.
 */
public class InvalidTokenException extends ApiException {

    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

}
