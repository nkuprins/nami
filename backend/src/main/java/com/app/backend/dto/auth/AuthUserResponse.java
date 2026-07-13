package com.app.backend.dto.auth;

import java.util.UUID;

public record AuthUserResponse(UUID id, String name, String email, boolean emailVerified, boolean admin) {}
