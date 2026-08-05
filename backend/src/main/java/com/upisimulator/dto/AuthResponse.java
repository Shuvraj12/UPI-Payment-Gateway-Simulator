package com.upisimulator.dto;

/**
 * Returned by register/login/refresh. {@code expiresIn} is in seconds
 * (OAuth2 convention), even though {@link com.upisimulator.security.JwtProperties}
 * stores expiry in milliseconds internally.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {

    public record UserSummary(Long id, String fullName, String email) {
    }

}
