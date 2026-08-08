package com.upisimulator.dto;

import com.upisimulator.entity.AccountType;
import com.upisimulator.entity.BankName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AddBankAccountRequest(

        @NotBlank(message = "Account holder name is required")
        String accountHolderName,

        @NotNull(message = "Bank name is required")
        BankName bankName,

        @NotBlank(message = "Account number is required")
        @Pattern(regexp = "^\\d{9,18}$", message = "Account number must be 9-18 digits")
        String accountNumber,

        @NotBlank(message = "IFSC code is required")
        @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC code must be 11 characters, e.g. HDFC0001234")
        String ifscCode,

        @NotNull(message = "Account type is required")
        AccountType accountType

) {
}
