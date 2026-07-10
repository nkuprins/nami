package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.auth.AuthUserResponse;
import com.app.backend.dto.auth.LoginRequest;
import com.app.backend.entity.RefreshToken;
import com.app.backend.entity.User;
import com.app.backend.exception.AuthException;
import com.app.backend.repository.RefreshTokenRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.CookieFactory;
import com.app.backend.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Session/authentication: login, refresh, logout, and the current-user lookup. Account lifecycle lives in {@link AccountService}. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieFactory cookieFactory;
    private final AppProperties props;

    @Transactional
    public AuthUserResponse login(LoginRequest req, HttpServletResponse response) {
        User user = userRepository.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new AuthException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Email not verified");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        setAuthCookies(user, response);
        return toResponse(user);
    }

    @Transactional
    public AuthUserResponse refresh(String rawToken, HttpServletResponse response) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Refresh token missing");
        }

        String hash = jwtService.hashToken(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid refresh token"));

        if (token.isRevoked() || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Refresh token expired");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        User user = token.getUser();
        setAuthCookies(user, response);
        return toResponse(user);
    }

    @Transactional
    public void logout(String rawToken, HttpServletResponse response) {
        if (rawToken != null && !rawToken.isBlank()) {
            refreshTokenRepository.findByTokenHash(jwtService.hashToken(rawToken))
                    .ifPresent(t -> {
                        t.setRevoked(true);
                        refreshTokenRepository.save(t);
                    });
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearAccessToken().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearRefreshToken().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearHasSessionCookie().toString());
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "User not found"));
        return toResponse(user);
    }

    private void setAuthCookies(User user, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(user.getId());
        String rawRefresh = jwtService.generateOpaqueToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(jwtService.hashToken(rawRefresh));
        refreshToken.setExpiresAt(OffsetDateTime.now().plusSeconds(props.jwt().refreshTokenTtlSeconds()));
        refreshTokenRepository.save(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.accessTokenCookie(accessToken).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.refreshTokenCookie(rawRefresh).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.hasSessionCookie().toString());
    }

    private AuthUserResponse toResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.isEmailVerified());
    }
}
