package com.upisimulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateUpiIdRequest(

        @NotBlank(message = "Username is required")
        @Pattern(
                regexp = "^[a-zA-Z][a-zA-Z0-9._-]{2,29}$",
                message = "Must start with a letter, 3-30 characters (letters, numbers, dots, underscores, hyphens only)"
        )
        String username

) {
}
