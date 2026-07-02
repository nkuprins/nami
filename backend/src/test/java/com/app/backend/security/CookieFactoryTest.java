package com.app.backend.security;

import com.app.backend.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

class CookieFactoryTest {

    private CookieFactory factory(boolean secure) {
        AppProperties props = new AppProperties(
                new AppProperties.CorsProperties("http://localhost"),
                new AppProperties.S3Properties("bucket", "us-east-1", 5, "https://cdn.test"),
                new AppProperties.JwtProperties("secret-32chars-long-enough-for-hmac", 900, 604800),
                new AppProperties.ResendProperties("", "test@test.local"),
                "http://localhost:3000",
                new AppProperties.CookieProperties(secure),
                null
        );
        return new CookieFactory(props);
    }

    @Test
    void accessTokenCookie_setsCorrectAttributes() {
        ResponseCookie cookie = factory(true).accessTokenCookie("jwt-value");

        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEqualTo("jwt-value");
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(900);
    }

    @Test
    void refreshTokenCookie_setsCorrectAttributes() {
        ResponseCookie cookie = factory(true).refreshTokenCookie("refresh-value");

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEqualTo("refresh-value");
        assertThat(cookie.getPath()).isEqualTo("/api/auth/refresh");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(604800);
    }

    @Test
    void clearAccessToken_setsMaxAgeToZero() {
        ResponseCookie cookie = factory(false).clearAccessToken();

        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
    }

    @Test
    void clearRefreshToken_setsMaxAgeToZero() {
        ResponseCookie cookie = factory(false).clearRefreshToken();

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
    }

    @Test
    void cookies_respectSecureFlag_whenFalse() {
        ResponseCookie cookie = factory(false).accessTokenCookie("value");

        assertThat(cookie.isSecure()).isFalse();
    }
}
