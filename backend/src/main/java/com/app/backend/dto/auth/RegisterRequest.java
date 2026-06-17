package com.app.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @Size(min = 15) @NotBlank String password
) {}
