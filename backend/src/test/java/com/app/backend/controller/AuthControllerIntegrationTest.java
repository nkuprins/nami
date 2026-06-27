package com.app.backend.controller;

import com.app.backend.IntegrationTestBase;
import com.app.backend.dto.auth.*;
import com.app.backend.entity.EmailVerificationToken;
import com.app.backend.entity.User;
import com.app.backend.repository.EmailVerificationTokenRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private User createVerifiedUser(String email) {
        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private User createUnverifiedUser(String email) {
        User user = new User();
        user.setName("Unverified User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        user.setEmailVerified(false);
        return userRepository.save(user);
    }

    @Test
    void register_returns201_withUserResponse() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "alice@test.com", "TestPassword12345");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    void register_returns409_whenEmailTaken() throws Exception {
        createVerifiedUser("taken@test.com");
        RegisterRequest req = new RegisterRequest("Bob", "taken@test.com", "TestPassword12345");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void register_returns400_whenPasswordTooShort() throws Exception {
        RegisterRequest req = new RegisterRequest("Charlie", "charlie@test.com", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns200_withCookies_whenCredentialsValid() throws Exception {
        createVerifiedUser("login@test.com");
        LoginRequest req = new LoginRequest("login@test.com", "TestPassword12345");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@test.com"))
                .andReturn();

        assertThat(result.getResponse().getCookies()).extracting("name")
                .contains("access_token", "refresh_token");
    }

    @Test
    void login_returns401_whenInvalidCredentials() throws Exception {
        createVerifiedUser("loginbad@test.com");
        LoginRequest req = new LoginRequest("loginbad@test.com", "WrongPassword12345");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_returns403_whenEmailNotVerified() throws Exception {
        createUnverifiedUser("unverified@test.com");
        LoginRequest req = new LoginRequest("unverified@test.com", "TestPassword12345");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void me_returns200_whenAuthenticated() throws Exception {
        User user = createVerifiedUser("me@test.com");

        mockMvc.perform(get("/api/auth/me")
                        .cookie(authTestHelper.accessTokenCookie(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@test.com"));
    }

    @Test
    void me_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returns401_whenNoCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_returns204_andClearsCookies() throws Exception {
        User user = createVerifiedUser("logout@test.com");

        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .cookie(authTestHelper.accessTokenCookie(user.getId())))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie clearAccess = result.getResponse().getCookie("access_token");
        Cookie clearRefresh = result.getResponse().getCookie("refresh_token");
        assertThat(clearAccess).isNotNull();
        assertThat(clearAccess.getMaxAge()).isZero();
        assertThat(clearRefresh).isNotNull();
        assertThat(clearRefresh.getMaxAge()).isZero();
    }

    @Test
    void deleteAccount_returns204_andRemovesUser() throws Exception {
        User user = createVerifiedUser("delete@test.com");
        UUID userId = user.getId();

        mockMvc.perform(delete("/api/auth/me")
                        .cookie(authTestHelper.accessTokenCookie(userId)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    void verifyEmail_returns204_whenTokenValid() throws Exception {
        User user = createUnverifiedUser("verify@test.com");

        String raw = jwtService.generateOpaqueToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(jwtService.hashToken(raw));
        token.setExpiresAt(OffsetDateTime.now().plusHours(24));
        emailVerificationTokenRepository.save(token);

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyEmailRequest(raw))))
                .andExpect(status().isNoContent());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.isEmailVerified()).isTrue();
    }

    @Test
    void forgotPassword_alwaysReturns204() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("ghost@test.com"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void resendVerification_alwaysReturns204() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest("anyone@test.com"))))
                .andExpect(status().isNoContent());
    }
}
