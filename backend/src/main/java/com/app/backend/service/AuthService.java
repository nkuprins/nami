package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.auth.*;
import com.app.backend.entity.*;
import com.app.backend.exception.AuthException;
import com.app.backend.repository.*;
import com.app.backend.security.CookieFactory;
import com.app.backend.security.JwtService;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieFactory cookieFactory;
    private final EmailService emailService;
    private final UploadService uploadService;
    private final AppProperties props;

    @Transactional
    public AuthUserResponse register(RegisterRequest req) {
        if (userRepository.findByEmailIgnoreCase(req.email()).isPresent()) {
            throw new AuthException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email already in use");
        }

        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setEmailVerified(false);
        userRepository.save(user);

        sendVerificationEmail(user);

        return toResponse(user);
    }

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
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "User not found"));
        return toResponse(user);
    }

    @Transactional
    public void deleteAccount(UUID userId, HttpServletResponse response) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        List<String> allPhotoUrls = propertyRepository.findByOwner(user).stream()
                .flatMap(p -> p.getPhotos().stream())
                .map(PropertyPhoto::getUrl)
                .toList();

        userRepository.delete(user);

        if (!allPhotoUrls.isEmpty()) {
            try {
                uploadService.deleteObjects(allPhotoUrls);
            } catch (Exception e) {
                log.warn("Failed to delete S3 objects for user {}: {}", userId, e.getMessage());
            }
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearAccessToken().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearRefreshToken().toString());
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest req) {
        String hash = jwtService.hashToken(req.token());
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Invalid or already used verification token"));

        if (token.isUsed()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Invalid or already used verification token");
        }
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED", "Verification link has expired");
        }

        token.setUsed(true);
        emailVerificationTokenRepository.save(token);

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest req) {
        userRepository.findByEmailIgnoreCase(req.email()).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                emailVerificationTokenRepository.deleteByUser(user);
                sendVerificationEmail(user);
            }
        });
        // Always return 200 - never reveal whether the email exists
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmailIgnoreCase(req.email()).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUser(user);

            String raw = jwtService.generateOpaqueToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(jwtService.hashToken(raw));
            token.setExpiresAt(OffsetDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(token);

            String link = props.frontendUrl() + "/reset-password?token=" + raw;
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), link);
        });
        // Always return 200
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String hash = jwtService.hashToken(req.token());
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Invalid or already used reset token"));

        if (token.isUsed()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Invalid or already used reset token");
        }
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED", "Reset link has expired");
        }

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        // Invalidate all existing sessions
        refreshTokenRepository.deleteByUser(user);
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
    }

    private void sendVerificationEmail(User user) {
        String raw = jwtService.generateOpaqueToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(jwtService.hashToken(raw));
        token.setExpiresAt(OffsetDateTime.now().plusHours(24));
        emailVerificationTokenRepository.save(token);

        String link = props.frontendUrl() + "/verify-email?token=" + raw;
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), link);
    }

    private AuthUserResponse toResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.isEmailVerified());
    }
}
