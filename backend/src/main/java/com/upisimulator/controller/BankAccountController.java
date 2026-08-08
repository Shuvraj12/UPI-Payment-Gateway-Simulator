package com.upisimulator.controller;

import com.upisimulator.dto.AddBankAccountRequest;
import com.upisimulator.dto.ApiResponse;
import com.upisimulator.dto.BankAccountResponse;
import com.upisimulator.entity.User;
import com.upisimulator.service.BankAccountService;
import com.upisimulator.util.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Every method acts only on {@code currentUser}'s own accounts.
 * {@code accountId} is client-supplied (unlike Wallet/Profile's implicit
 * "my own singular resource"), so every lookup goes through
 * {@code BankAccountService}'s {@code findByIdAndUserId}-backed methods -
 * see {@code BankAccountRepository}'s Javadoc for why that matters.
 */
@RestController
@RequestMapping(ApiPaths.BASE + "/bank-accounts")
@RequiredArgsConstructor
@Tag(name = "Bank Accounts", description = "Linked bank accounts: add, verify, set primary, delete")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping
    @Operation(summary = "Link a new bank account (first one becomes primary automatically)")
    public ResponseEntity<ApiResponse<BankAccountResponse>> addBankAccount(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AddBankAccountRequest request) {
        BankAccountResponse response = bankAccountService.addBankAccount(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Bank account added", response));
    }

    @GetMapping
    @Operation(summary = "List linked bank accounts")
    public ResponseEntity<ApiResponse<List<BankAccountResponse>>> getBankAccounts(
            @AuthenticationPrincipal User currentUser) {
        List<BankAccountResponse> response = bankAccountService.getBankAccounts(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Bank accounts retrieved", response));
    }

    @PostMapping("/{accountId}/verify")
    @Operation(summary = "Simulate a verification check (~85% success rate, can retry on failure)")
    public ResponseEntity<ApiResponse<BankAccountResponse>> verifyBankAccount(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long accountId) {
        BankAccountResponse response = bankAccountService.verifyBankAccount(currentUser.getId(), accountId);
        return ResponseEntity.ok(ApiResponse.success("Verification attempted", response));
    }

    @PutMapping("/{accountId}/primary")
    @Operation(summary = "Set a verified account as primary")
    public ResponseEntity<ApiResponse<BankAccountResponse>> setPrimary(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long accountId) {
        BankAccountResponse response = bankAccountService.setPrimary(currentUser.getId(), accountId);
        return ResponseEntity.ok(ApiResponse.success("Primary account updated", response));
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "Remove a linked bank account")
    public ResponseEntity<ApiResponse<Void>> deleteBankAccount(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long accountId) {
        bankAccountService.deleteBankAccount(currentUser.getId(), accountId);
        return ResponseEntity.ok(ApiResponse.success("Bank account deleted", null));
    }

}
