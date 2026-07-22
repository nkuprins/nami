package com.app.backend.security;

import com.app.backend.config.AppProperties;
import com.app.backend.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Set;

/**
 * Verifies Google Identity Services ID tokens. Signature and key rotation are handled by
 * {@link NimbusJwtDecoder} against Google's published JWK set; on top of the default timestamp
 * check we enforce Google's issuer and that the token was minted for our OAuth client id.
 */
@Component
public class GoogleTokenVerifier {

    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> VALID_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final NimbusJwtDecoder decoder;

    public GoogleTokenVerifier(AppProperties props) {
        String clientId = props.google().clientId();
        this.decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                issuerValidator(),
                audienceValidator(clientId)));
    }

    public GoogleUser verify(String idToken) {
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Invalid Google token");
        }

        String email = jwt.getClaimAsString("email");
        boolean emailVerified = Boolean.TRUE.equals(jwt.getClaim("email_verified"))
                || "true".equalsIgnoreCase(jwt.getClaimAsString("email_verified"));
        if (email == null || !emailVerified) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "GOOGLE_EMAIL_UNVERIFIED", "Google account email is not verified");
        }

        return new GoogleUser(jwt.getSubject(), email.toLowerCase(), jwt.getClaimAsString("name"));
    }

    private static OAuth2TokenValidator<Jwt> issuerValidator() {
        return jwt -> {
            URL issuer = jwt.getIssuer();
            if (issuer != null && VALID_ISSUERS.contains(issuer.toString())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_issuer", "Untrusted token issuer", null));
        };
    }

    private static OAuth2TokenValidator<Jwt> audienceValidator(String clientId) {
        return jwt -> jwt.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_audience", "Token audience mismatch", null));
    }
}
