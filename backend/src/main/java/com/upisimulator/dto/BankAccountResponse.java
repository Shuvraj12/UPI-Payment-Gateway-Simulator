package com.upisimulator.dto;

import com.upisimulator.entity.AccountType;
import com.upisimulator.entity.BankName;
import com.upisimulator.entity.VerificationStatus;

import java.time.LocalDateTime;

public record BankAccountResponse(
        Long id,
        String accountHolderName,
        BankName bankName,
        String maskedAccountNumber,
        String ifscCode,
        AccountType accountType,
        boolean primary,
        VerificationStatus verificationStatus,
        LocalDateTime verifiedAt,
        LocalDateTime createdAt
) {
}
