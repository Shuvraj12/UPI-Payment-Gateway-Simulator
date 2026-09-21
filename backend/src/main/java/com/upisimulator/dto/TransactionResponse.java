package com.upisimulator.dto;

import com.upisimulator.entity.TransactionDirection;
import com.upisimulator.entity.TransactionStatus;
import com.upisimulator.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String referenceNumber,
        TransactionType type,
        TransactionDirection direction,
        BigDecimal amount,
        BigDecimal balanceAfter,
        TransactionStatus status,
        String description,
        String counterpartyVpa,
        LocalDateTime createdAt
) {
}
