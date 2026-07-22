package com.app.backend.controller;

import com.app.backend.dto.export.UserExportDto;
import com.app.backend.dto.auth.*;
import com.app.backend.service.AccountService;
import com.app.backend.service.AuthService;
import com.app.backend.service.TurnstileService;
import com.app.backend.service.UserDataExportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountService accountService;
    private final UserDataExportService userDataExportService;
    private final TurnstileService turnstileService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthUserResponse register(
            @RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken,
            @RequestBody @Valid RegisterRequest req,
            HttpServletRequest httpRequest) {
        turnstileService.verify(turnstileToken, clientIp(httpRequest));
        return accountService.register(req);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/login")
    public AuthUserResponse login(@RequestBody @Valid LoginRequest req, HttpServletResponse response) {
        return authService.login(req, response);
    }

    @PostMapping("/google")
    public AuthUserResponse google(@RequestBody @Valid GoogleLoginRequest req, HttpServletResponse response) {
        return authService.loginWithGoogle(req.credential(), response);
    }

    @PostMapping("/refresh")
    public AuthUserResponse refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        return authService.refresh(refreshToken, response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken, response);
    }

    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal UUID userId) {
        return authService.me(userId);
    }

    @PatchMapping("/me")
    public AuthUserResponse updateProfile(@AuthenticationPrincipal UUID userId,
                                          @RequestBody @Valid UpdateProfileRequest req) {
        return accountService.updateProfile(userId, req);
    }

    @GetMapping("/export")
    public UserExportDto export(@AuthenticationPrincipal UUID userId) {
        return userDataExportService.export(userId);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal UUID userId, HttpServletResponse response) {
        accountService.deleteAccount(userId, response);
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@RequestBody @Valid VerifyEmailRequest req) {
        accountService.verifyEmail(req);
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification(@RequestBody @Valid ResendVerificationRequest req) {
        accountService.resendVerification(req);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@RequestBody @Valid ForgotPasswordRequest req) {
        accountService.forgotPassword(req);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        accountService.resetPassword(req);
    }
}
