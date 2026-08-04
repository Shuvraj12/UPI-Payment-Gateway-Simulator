package com.upisimulator.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Standard error envelope returned by {@link com.upisimulator.exception.GlobalExceptionHandler}.
 * Keeping this separate from {@link ApiResponse} avoids a generic {@code <T>}
 * that's always {@code null} on the error path.
 */
@Getter
@Builder
public class ErrorResponse {

    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final LocalDateTime timestamp;

}
