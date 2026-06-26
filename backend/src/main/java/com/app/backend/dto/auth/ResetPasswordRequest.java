package com.app.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 64) String token,
        @NotBlank @Size(min = 15, max = 128) String newPassword
) {}
