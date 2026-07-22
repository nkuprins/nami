package com.app.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** The Google ID token (JWT credential) returned by Google Identity Services on the client. */
public record GoogleLoginRequest(@NotBlank String credential) {}
