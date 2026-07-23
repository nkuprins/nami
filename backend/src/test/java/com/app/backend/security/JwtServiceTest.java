package com.app.backend.security;

import com.app.backend.config.AppProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppProperties props = AppProperties.builder()
                .cors(new AppProperties.CorsProperties("http://localhost"))
                .s3(new AppProperties.S3Properties("bucket", "us-east-1", 5, "https://cdn.test"))
                .jwt(new AppProperties.JwtProperties("test-secret-must-be-at-least-32-characters-long-for-hs256", 900, 604800))
                .resend(new AppProperties.ResendProperties("", "test@test.local"))
                .frontendUrl("http://localhost:3000")
                .cookie(new AppProperties.CookieProperties(false))
                .build();
        jwtService = new JwtService(props);
    }

    @Test
    void generateAccessToken_returnsNonBlankJwt() {
        String token = jwtService.generateAccessToken(UUID.randomUUID());

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void parseUserId_returnsCorrectUUID() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId);

        UUID parsed = jwtService.parseUserId(token);

        assertThat(parsed).isEqualTo(userId);
    }

    @Test
    void parseUserId_throwsWhenTokenExpired() {
        AppProperties expiredProps = AppProperties.builder()
                .cors(new AppProperties.CorsProperties("http://localhost"))
                .s3(new AppProperties.S3Properties("bucket", "us-east-1", 5, "https://cdn.test"))
                .jwt(new AppProperties.JwtProperties("test-secret-must-be-at-least-32-characters-long-for-hs256", 0, 0))
                .resend(new AppProperties.ResendProperties("", "test@test.local"))
                .frontendUrl("http://localhost:3000")
                .cookie(new AppProperties.CookieProperties(false))
                .build();
        JwtService expiredService = new JwtService(expiredProps);
        String token = expiredService.generateAccessToken(UUID.randomUUID());

        assertThatThrownBy(() -> jwtService.parseUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseUserId_throwsWhenSignatureInvalid() {
        AppProperties otherProps = AppProperties.builder()
                .cors(new AppProperties.CorsProperties("http://localhost"))
                .s3(new AppProperties.S3Properties("bucket", "us-east-1", 5, "https://cdn.test"))
                .jwt(new AppProperties.JwtProperties("different-secret-must-be-at-least-32-characters-long", 900, 604800))
                .resend(new AppProperties.ResendProperties("", "test@test.local"))
                .frontendUrl("http://localhost:3000")
                .cookie(new AppProperties.CookieProperties(false))
                .build();
        JwtService otherService = new JwtService(otherProps);
        String token = otherService.generateAccessToken(UUID.randomUUID());

        assertThatThrownBy(() -> jwtService.parseUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void generateOpaqueToken_returns64HexChars() {
        String token = jwtService.generateOpaqueToken();

        assertThat(token).hasSize(64);
        assertThat(token).matches("[0-9a-f]+");
    }

    @Test
    void generateOpaqueToken_isUnique() {
        String token1 = jwtService.generateOpaqueToken();
        String token2 = jwtService.generateOpaqueToken();

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void hashToken_returnsDeterministicSHA256() {
        String hash1 = jwtService.hashToken("test-input");
        String hash2 = jwtService.hashToken("test-input");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
        assertThat(hash1).matches("[0-9a-f]+");
    }

    @Test
    void hashToken_differentInputsProduceDifferentHashes() {
        String hash1 = jwtService.hashToken("input-a");
        String hash2 = jwtService.hashToken("input-b");

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
