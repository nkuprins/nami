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
        return base("access_token", token, "/api", props.jwt().accessTokenTtlSeconds(), true);
    }

    public ResponseCookie refreshTokenCookie(String token) {
        return base("refresh_token", token, "/api/auth/refresh", props.jwt().refreshTokenTtlSeconds(), true);
    }

    public ResponseCookie clearAccessToken() {
        return base("access_token", "", "/api", 0, true);
    }

    public ResponseCookie clearRefreshToken() {
        return base("refresh_token", "", "/api/auth/refresh", 0, true);
    }

    /**
     * Non-httpOnly hint cookie: lets the frontend skip /auth/me for visitors who
     * have never logged in, without exposing the actual session tokens to JS.
     */
    public ResponseCookie hasSessionCookie() {
        return base("has_session", "1", "/", props.jwt().refreshTokenTtlSeconds(), false);
    }

    public ResponseCookie clearHasSessionCookie() {
        return base("has_session", "", "/", 0, false);
    }

    private ResponseCookie base(String name, String value, String path, long maxAge, boolean httpOnly) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(props.cookie().secure())
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
