package com.app.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(@NotBlank String token) {}
