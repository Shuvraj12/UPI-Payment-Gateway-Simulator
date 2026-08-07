package com.upisimulator.controller;

import com.upisimulator.dto.ApiResponse;
import com.upisimulator.dto.DepositRequest;
import com.upisimulator.dto.TransactionResponse;
import com.upisimulator.dto.WalletResponse;
import com.upisimulator.entity.User;
import com.upisimulator.service.WalletService;
import com.upisimulator.util.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Every method acts only on {@code currentUser.getId()}'s own wallet - same
 * reasoning as {@code ProfileController}, there's no other wallet a caller
 * could reach through this controller.
 */
@RestController
@RequestMapping(ApiPaths.BASE + "/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Wallet balance, simulated deposits, freeze, and transaction ledger")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Create a wallet (once per account)")
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(@AuthenticationPrincipal User currentUser) {
        WalletResponse response = walletService.createWallet(currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Wallet created", response));
    }

    @GetMapping
    @Operation(summary = "Get balance and freeze status")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@AuthenticationPrincipal User currentUser) {
        WalletResponse response = walletService.getWallet(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Wallet retrieved", response));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Simulated deposit - capped test/demo funding, not a real payment rail")
    public ResponseEntity<ApiResponse<WalletResponse>> deposit(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody DepositRequest request) {
        WalletResponse response = walletService.deposit(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Deposit successful", response));
    }

    @PutMapping("/freeze")
    @Operation(summary = "Freeze your own wallet (blocks deposits; will block transfers from Phase 7)")
    public ResponseEntity<ApiResponse<WalletResponse>> freeze(@AuthenticationPrincipal User currentUser) {
        WalletResponse response = walletService.freeze(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Wallet frozen", response));
    }

    @PutMapping("/unfreeze")
    @Operation(summary = "Unfreeze your own wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> unfreeze(@AuthenticationPrincipal User currentUser) {
        WalletResponse response = walletService.unfreeze(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Wallet unfrozen", response));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Paginated transaction ledger for this wallet")
    public ResponseEntity<ApiResponse<PagedModel<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        var page = walletService.getTransactions(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved", new PagedModel<>(page)));
    }

}
