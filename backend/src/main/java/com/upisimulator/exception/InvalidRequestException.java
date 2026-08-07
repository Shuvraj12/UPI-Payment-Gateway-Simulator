package com.upisimulator.exception;

import org.springframework.http.HttpStatus;

/**
 * For requests that pass bean validation but still violate a business rule
 * (e.g. "new password same as current password") - a different category
 * from the {@code @Valid} annotation failures {@link GlobalExceptionHandler}
 * handles separately. Mapped to 400.
 */
public class InvalidRequestException extends ApiException {

    public InvalidRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

}
