package com.upisimulator.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long id,
        BigDecimal balance,
        boolean frozen,
        LocalDateTime createdAt
) {
}
