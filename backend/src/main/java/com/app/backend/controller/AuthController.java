package com.app.backend.controller;

import com.app.backend.dto.UserExportDto;
import com.app.backend.dto.auth.*;
import com.app.backend.service.AuthService;
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

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthUserResponse register(@RequestBody @Valid RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthUserResponse login(@RequestBody @Valid LoginRequest req, HttpServletResponse response) {
        return authService.login(req, response);
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
        return authService.updateProfile(userId, req);
    }

    @GetMapping("/export")
    public UserExportDto export(@AuthenticationPrincipal UUID userId) {
        return authService.export(userId);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal UUID userId, HttpServletResponse response) {
        authService.deleteAccount(userId, response);
    }

    @PostMapping("/verify-email")
    public void verifyEmail(@RequestBody @Valid VerifyEmailRequest req) {
        authService.verifyEmail(req);
    }

    @PostMapping("/resend-verification")
    public void resendVerification(@RequestBody @Valid ResendVerificationRequest req) {
        authService.resendVerification(req);
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@RequestBody @Valid ForgotPasswordRequest req) {
        authService.forgotPassword(req);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        authService.resetPassword(req);
    }
}
