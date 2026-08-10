package com.upisimulator.controller;

import com.upisimulator.dto.ApiResponse;
import com.upisimulator.dto.CreateUpiIdRequest;
import com.upisimulator.dto.UpiIdAvailabilityResponse;
import com.upisimulator.dto.UpiIdResponse;
import com.upisimulator.entity.User;
import com.upisimulator.service.UpiIdService;
import com.upisimulator.util.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.BASE + "/upi-ids")
@RequiredArgsConstructor
@Tag(name = "UPI IDs", description = "Create, list, check availability, and set a default UPI ID")
public class UpiIdController {

    private final UpiIdService upiIdService;

    @PostMapping
    @Operation(summary = "Create a UPI ID (requires an existing wallet and a verified bank account)")
    public ResponseEntity<ApiResponse<UpiIdResponse>> createUpiId(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateUpiIdRequest request) {
        UpiIdResponse response = upiIdService.createUpiId(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("UPI ID created", response));
    }

    @GetMapping
    @Operation(summary = "List your UPI IDs")
    public ResponseEntity<ApiResponse<List<UpiIdResponse>>> getUpiIds(@AuthenticationPrincipal User currentUser) {
        List<UpiIdResponse> response = upiIdService.getUpiIds(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("UPI IDs retrieved", response));
    }

    @GetMapping("/availability")
    @Operation(summary = "Check whether a username is available, for live-typing checks")
    public ResponseEntity<ApiResponse<UpiIdAvailabilityResponse>> checkAvailability(
            @RequestParam String username) {
        UpiIdAvailabilityResponse response = upiIdService.checkAvailability(username);
        return ResponseEntity.ok(ApiResponse.success("Availability checked", response));
    }

    @PutMapping("/{upiIdId}/default")
    @Operation(summary = "Set one of your UPI IDs as the default")
    public ResponseEntity<ApiResponse<UpiIdResponse>> setDefault(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long upiIdId) {
        UpiIdResponse response = upiIdService.setDefault(currentUser.getId(), upiIdId);
        return ResponseEntity.ok(ApiResponse.success("Default UPI ID updated", response));
    }

}
