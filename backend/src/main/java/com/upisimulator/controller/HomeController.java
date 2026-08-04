package com.upisimulator.controller;

import com.upisimulator.dto.ApiResponse;
import com.upisimulator.util.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 1's one functional endpoint. Its only job is to prove the React
 * frontend and Spring Boot backend can actually reach each other before any
 * real domain logic (wallets, transfers, ...) gets built on top.
 */
@RestController
@RequestMapping(ApiPaths.BASE)
@Tag(name = "Home", description = "Application health and metadata")
public class HomeController {

    @Value("${spring.application.name}")
    private String applicationName;

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Confirms the backend is up and reachable from the frontend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("application", applicationName);
        data.put("status", "UP");
        data.put("reference", "HLTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        data.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(ApiResponse.success("Service is healthy", data));
    }

}
