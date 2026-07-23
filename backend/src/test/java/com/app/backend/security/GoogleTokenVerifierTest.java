package com.app.backend.security;

import com.app.backend.config.AppProperties;
import com.app.backend.exception.AuthException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleTokenVerifierTest {

    private GoogleTokenVerifier verifier() {
        AppProperties props = AppProperties.builder()
                .google(new AppProperties.GoogleProperties("test-client-id"))
                .build();
        return new GoogleTokenVerifier(props);
    }

    @Test
    void verify_throwsInvalidGoogleToken_whenTokenMalformed() {
        // A non-JWS string fails Nimbus parsing before any JWKS network lookup.
        assertThatThrownBy(() -> verifier().verify("not-a-jwt"))
                .isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getCode()).isEqualTo("INVALID_GOOGLE_TOKEN"));
    }
}
