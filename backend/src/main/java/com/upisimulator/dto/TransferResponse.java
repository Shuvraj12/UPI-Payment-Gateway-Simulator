package com.upisimulator.dto;

import com.upisimulator.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        String referenceNumber,
        String recipientVpa,
        BigDecimal amount,
        BigDecimal senderBalanceAfter,
        TransactionStatus status,
        LocalDateTime createdAt
) {
}
