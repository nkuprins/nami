package com.app.backend.testutil;

import com.app.backend.security.JwtService;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthTestHelper {

    private final JwtService jwtService;

    public Cookie accessTokenCookie(UUID userId) {
        String token = jwtService.generateAccessToken(userId);
        return new Cookie("access_token", token);
    }

    public Cookie refreshTokenCookie(String rawToken) {
        return new Cookie("refresh_token", rawToken);
    }
}
