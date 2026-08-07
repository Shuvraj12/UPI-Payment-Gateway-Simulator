package com.upisimulator.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(

        @NotBlank(message = "Password is required to delete your account")
        String password

) {
}
