package com.upisimulator.controller;

import com.upisimulator.dto.ApiResponse;
import com.upisimulator.dto.ResolveRecipientResponse;
import com.upisimulator.dto.TransferRequest;
import com.upisimulator.dto.TransferResponse;
import com.upisimulator.entity.User;
import com.upisimulator.service.TransferService;
import com.upisimulator.util.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Send money to a UPI ID; resolve a recipient's name before sending")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Send money to a UPI ID (idempotency key required)")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.transfer(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Transfer completed", response));
    }

    @GetMapping("/resolve")
    @Operation(summary = "Look up the account name behind a UPI ID, to confirm before sending")
    public ResponseEntity<ApiResponse<ResolveRecipientResponse>> resolveRecipient(@RequestParam String vpa) {
        ResolveRecipientResponse response = transferService.resolveRecipient(vpa);
        return ResponseEntity.ok(ApiResponse.success("Recipient resolved", response));
    }

}
