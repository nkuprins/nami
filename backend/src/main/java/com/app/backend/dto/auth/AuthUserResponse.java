package com.app.backend.dto.auth;

import java.util.UUID;
import lombok.Builder;

@Builder
public record AuthUserResponse(UUID id, String name, String email, boolean emailVerified, boolean admin) {}
