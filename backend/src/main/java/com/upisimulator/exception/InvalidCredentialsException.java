package com.upisimulator.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown on a failed login (wrong password, or a disabled/frozen account).
 * Mapped to 401. Deliberately doesn't distinguish "wrong email" from "wrong
 * password" in the message - that distinction just helps an attacker
 * enumerate valid emails.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

}
