package com.upisimulator.dto;

import java.time.LocalDateTime;

public record UpiIdResponse(
        Long id,
        String vpa,
        boolean isDefault,
        LocalDateTime createdAt
) {
}
