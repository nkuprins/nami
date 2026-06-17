package com.app.backend.security;

import com.app.backend.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieFactory {

    private final AppProperties props;

    public ResponseCookie accessTokenCookie(String token) {
        return base("access_token", token, "/api", props.jwt().accessTokenTtlSeconds());
    }

    public ResponseCookie refreshTokenCookie(String token) {
        return base("refresh_token", token, "/api/auth/refresh", props.jwt().refreshTokenTtlSeconds());
    }

    public ResponseCookie clearAccessToken() {
        return base("access_token", "", "/api", 0);
    }

    public ResponseCookie clearRefreshToken() {
        return base("refresh_token", "", "/api/auth/refresh", 0);
    }

    private ResponseCookie base(String name, String value, String path, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(props.cookie().secure())
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
