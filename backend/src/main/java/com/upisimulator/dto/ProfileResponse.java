package com.upisimulator.dto;

import java.time.LocalDateTime;

public record ProfileResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        String profilePictureUrl,
        LocalDateTime memberSince
) {
}
