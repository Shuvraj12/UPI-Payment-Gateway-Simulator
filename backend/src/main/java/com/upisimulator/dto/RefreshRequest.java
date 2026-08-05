package com.upisimulator.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Used by both POST /auth/refresh (exchange for a new access token) and
 * POST /auth/logout (revoke) - the request shape is identical either way.
 */
public record RefreshRequest(

        @NotBlank(message = "Refresh token is required")
        String refreshToken

) {
}
