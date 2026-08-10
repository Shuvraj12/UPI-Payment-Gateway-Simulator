package com.upisimulator.dto;

public record UpiIdAvailabilityResponse(
        String vpa,
        boolean available,
        String reason
) {
}
