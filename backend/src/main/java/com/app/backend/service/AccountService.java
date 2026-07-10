package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.auth.AuthUserResponse;
import com.app.backend.dto.auth.ForgotPasswordRequest;
import com.app.backend.dto.auth.RegisterRequest;
import com.app.backend.dto.auth.ResendVerificationRequest;
import com.app.backend.dto.auth.ResetPasswordRequest;
import com.app.backend.dto.auth.UpdateProfileRequest;
import com.app.backend.dto.auth.VerifyEmailRequest;
import com.app.backend.entity.EmailVerificationToken;
import com.app.backend.entity.PasswordResetToken;
import com.app.backend.entity.User;
import com.app.backend.exception.AuthException;
import com.app.backend.repository.EmailVerificationTokenRepository;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.PasswordResetTokenRepository;
import com.app.backend.repository.RefreshTokenRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.CookieFactory;
import com.app.backend.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Account lifecycle: registration, email verification, password reset, profile updates and deletion. Session/auth lives in {@link AuthService}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieFactory cookieFactory;
    private final EmailService emailService;
    private final MediaCleanupService mediaCleanupService;
    private final AppProperties props;

    @Transactional
    public AuthUserResponse register(RegisterRequest req) {
        if (userRepository.findByEmailIgnoreCase(req.email()).isPresent()) {
            throw new AuthException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email already in use");
        }

        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email().toLowerCase());
        user.setPasswordHash(Objects.requireNonNull(passwordEncoder.encode(req.password())));
        user.setEmailVerified(false);
        userRepository.save(user);
        log.info("User registered: {}", user.getId());

        sendVerificationEmail(user);

        return toResponse(user);
    }

    @Transactional
    public void deleteAccount(UUID userId, HttpServletResponse response) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        List<String> allFileUrls = listingRepository.findByOwner(user).stream()
                .flatMap(l -> l.allMediaUrls().stream())
                .toList();

        userRepository.delete(user);
        log.info("Account deleted: {}", userId);

        if (!allFileUrls.isEmpty()) {
            mediaCleanupService.enqueue(allFileUrls);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearAccessToken().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearRefreshToken().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearHasSessionCookie().toString());
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
        log.info("Email verified for user: {}", user.getId());
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest req) {
        userRepository.findByEmailIgnoreCase(req.email()).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                emailVerificationTokenRepository.deleteByUser(user);
                sendVerificationEmail(user);
            }
        });
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
            String email = user.getEmail();
            String name = user.getName();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.sendPasswordResetEmail(email, name, link);
                }
            });
        });
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
        user.setPasswordHash(Objects.requireNonNull(passwordEncoder.encode(req.newPassword())));
        userRepository.save(user);

        log.info("Password reset for user: {}", user.getId());
        // Invalidate all existing sessions
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public AuthUserResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "User not found"));

        boolean nameChanged = req.name() != null && !req.name().isBlank();
        boolean emailChanged = req.email() != null && !req.email().isBlank()
                && !req.email().equalsIgnoreCase(user.getEmail());

        if (!nameChanged && !emailChanged) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "NO_CHANGES", "No changes provided");
        }

        if (nameChanged) {
            user.setName(req.name().strip());
        }
        if (emailChanged) {
            String newEmail = req.email().strip().toLowerCase();
            if (userRepository.findByEmailIgnoreCase(newEmail).isPresent()) {
                throw new AuthException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email already in use");
            }
            user.setEmail(newEmail);
            user.setEmailVerified(false);
            refreshTokenRepository.deleteByUser(user);
            userRepository.save(user);
            sendVerificationEmail(user);
        } else {
            userRepository.save(user);
        }

        log.info("Profile updated for user: {}", userId);
        return toResponse(user);
    }

    private void sendVerificationEmail(User user) {
        String raw = jwtService.generateOpaqueToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(jwtService.hashToken(raw));
        token.setExpiresAt(OffsetDateTime.now().plusHours(24));
        emailVerificationTokenRepository.save(token);

        String link = props.frontendUrl() + "/verify-email?token=" + raw;
        String email = user.getEmail();
        String name = user.getName();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailService.sendVerificationEmail(email, name, link);
            }
        });
    }

    private AuthUserResponse toResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.isEmailVerified());
    }
}
