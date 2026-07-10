package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.auth.*;
import com.app.backend.entity.*;
import com.app.backend.exception.AuthException;
import com.app.backend.repository.*;
import com.app.backend.security.CookieFactory;
import com.app.backend.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.app.backend.testutil.TestData.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private CookieFactory cookieFactory;
    @Mock private EmailService emailService;
    @Mock private MediaCleanupService mediaCleanupService;
    @Mock private AppProperties props;

    @InjectMocks
    private AccountService accountService;

    @BeforeEach
    void initTransactionSync() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSync() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void triggerAfterCommit() {
        new ArrayList<>(TransactionSynchronizationManager.getSynchronizations())
                .forEach(TransactionSynchronization::afterCommit);
    }

    private void stubCookieFactory() {
        lenient().when(cookieFactory.clearAccessToken()).thenReturn(ResponseCookie.from("access_token", "").build());
        lenient().when(cookieFactory.clearRefreshToken()).thenReturn(ResponseCookie.from("refresh_token", "").build());
        lenient().when(cookieFactory.clearHasSessionCookie()).thenReturn(ResponseCookie.from("has_session", "").build());
    }

    private void stubTokenGeneration() {
        lenient().when(jwtService.generateOpaqueToken()).thenReturn("opaque-token");
        lenient().when(jwtService.hashToken(anyString())).thenReturn("hashed-token");
        lenient().when(props.frontendUrl()).thenReturn("http://localhost:3000");
    }

    @Nested
    class Register {
        @Test
        void savesUser_withLowercaseEmail_andEncodesPassword() {
            stubTokenGeneration();
            when(userRepository.findByEmailIgnoreCase("Test@Example.COM")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn("$2a$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            AuthUserResponse result = accountService.register(new RegisterRequest("Name", "Test@Example.COM", RAW_PASSWORD));

            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.emailVerified()).isFalse();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$encoded");
        }

        @Test
        void throwsConflict_whenEmailTaken() {
            when(userRepository.findByEmailIgnoreCase("taken@test.com")).thenReturn(Optional.of(user()));

            assertThatThrownBy(() -> accountService.register(registerRequest("taken@test.com")))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> {
                        AuthException ae = (AuthException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("EMAIL_TAKEN");
                    });
        }

        @Test
        void sendsVerificationEmail() {
            stubTokenGeneration();
            when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            accountService.register(registerRequest());
            triggerAfterCommit();

            verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
            verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        }
    }

    @Nested
    class DeleteAccount {

        private HttpServletResponse response;

        @BeforeEach
        void setUp() {
            response = mock(HttpServletResponse.class);
            stubCookieFactory();
        }

        @Test
        void deletesUser_andS3Photos_andClearsCookies() {
            User user = user();
            Listing listing = listingWithPhotos(user);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(listingRepository.findByOwner(user)).thenReturn(List.of(listing));

            accountService.deleteAccount(user.getId(), response);
            triggerAfterCommit();

            verify(userRepository).delete(user);
            verify(mediaCleanupService).enqueue(List.of(
                    "https://cdn.test.local/uploads/photo1.jpg",
                    "https://cdn.test.local/uploads/photo2.jpg"
            ));
            verify(response, times(3)).addHeader(eq("Set-Cookie"), anyString());
        }

        @Test
        void throwsNotFound_whenUserNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.deleteAccount(id, response))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        void doesNotCallS3_whenUserHasNoProperties() {
            User user = user();
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(listingRepository.findByOwner(user)).thenReturn(List.of());

            accountService.deleteAccount(user.getId(), response);

            verify(userRepository).delete(user);
            verify(mediaCleanupService, never()).enqueue(any());
        }
    }

    @Nested
    class VerifyEmail {
        @Test
        void marksTokenUsed_andUserVerified() {
            User user = unverifiedUser("verify@test.com");
            EmailVerificationToken token = emailVerificationToken(user);
            when(jwtService.hashToken("raw")).thenReturn("hash");
            when(emailVerificationTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

            accountService.verifyEmail(new VerifyEmailRequest("raw"));

            assertThat(token.isUsed()).isTrue();
            assertThat(user.isEmailVerified()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        void throwsBadRequest_whenTokenNotFound() {
            when(jwtService.hashToken("bad")).thenReturn("hash");
            when(emailVerificationTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.verifyEmail(new VerifyEmailRequest("bad")))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        void throwsBadRequest_whenTokenAlreadyUsed() {
            EmailVerificationToken token = emailVerificationToken(user());
            token.setUsed(true);
            when(jwtService.hashToken("used")).thenReturn("hash");
            when(emailVerificationTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> accountService.verifyEmail(new VerifyEmailRequest("used")))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        void throwsBadRequest_whenTokenExpired() {
            EmailVerificationToken token = emailVerificationToken(user());
            token.setExpiresAt(OffsetDateTime.now().minusHours(1));
            when(jwtService.hashToken("expired")).thenReturn("hash");
            when(emailVerificationTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> accountService.verifyEmail(new VerifyEmailRequest("expired")))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getCode()).isEqualTo("TOKEN_EXPIRED"));
        }
    }

    @Nested
    class ResendVerification {
        @Test
        void deletesOldTokens_andSendsNew_whenUnverified() {
            stubTokenGeneration();
            User user = unverifiedUser("unverified@test.com");
            when(userRepository.findByEmailIgnoreCase("unverified@test.com")).thenReturn(Optional.of(user));

            accountService.resendVerification(new ResendVerificationRequest("unverified@test.com"));
            triggerAfterCommit();

            verify(emailVerificationTokenRepository).deleteByUser(user);
            verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        void doesNothing_whenAlreadyVerified() {
            User user = user("verified@test.com");
            when(userRepository.findByEmailIgnoreCase("verified@test.com")).thenReturn(Optional.of(user));

            accountService.resendVerification(new ResendVerificationRequest("verified@test.com"));

            verify(emailVerificationTokenRepository, never()).deleteByUser(any());
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        void doesNothing_whenEmailNotFound() {
            when(userRepository.findByEmailIgnoreCase("ghost@test.com")).thenReturn(Optional.empty());

            assertThatCode(() -> accountService.resendVerification(new ResendVerificationRequest("ghost@test.com")))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class ForgotPassword {
        @Test
        void createsToken_andSendsEmail_whenUserExists() {
            stubTokenGeneration();
            User user = user("forgot@test.com");
            when(userRepository.findByEmailIgnoreCase("forgot@test.com")).thenReturn(Optional.of(user));

            accountService.forgotPassword(new ForgotPasswordRequest("forgot@test.com"));
            triggerAfterCommit();

            verify(passwordResetTokenRepository).deleteByUser(user);
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            verify(emailService).sendPasswordResetEmail(eq("forgot@test.com"), anyString(), anyString());
        }

        @Test
        void doesNothing_whenEmailNotFound() {
            when(userRepository.findByEmailIgnoreCase("ghost@test.com")).thenReturn(Optional.empty());

            assertThatCode(() -> accountService.forgotPassword(new ForgotPasswordRequest("ghost@test.com")))
                    .doesNotThrowAnyException();
            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    class ResetPassword {
        @Test
        void updatesHash_andInvalidatesAllSessions() {
            User user = user();
            PasswordResetToken token = passwordResetToken(user);
            when(jwtService.hashToken("raw")).thenReturn("hash");
            when(passwordResetTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("NewPassword123456")).thenReturn("$2a$new-hash");

            accountService.resetPassword(new ResetPasswordRequest("raw", "NewPassword123456"));

            assertThat(token.isUsed()).isTrue();
            assertThat(user.getPasswordHash()).isEqualTo("$2a$new-hash");
            verify(refreshTokenRepository).deleteByUser(user);
        }

        @Test
        void throwsBadRequest_whenTokenNotFound() {
            when(jwtService.hashToken("bad")).thenReturn("hash");
            when(passwordResetTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.resetPassword(new ResetPasswordRequest("bad", "NewPassword123456")))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        void throwsBadRequest_whenTokenUsed() {
            PasswordResetToken token = passwordResetToken(user());
            token.setUsed(true);
            when(jwtService.hashToken("used")).thenReturn("hash");
            when(passwordResetTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> accountService.resetPassword(new ResetPasswordRequest("used", "NewPassword123456")))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        void throwsBadRequest_whenTokenExpired() {
            PasswordResetToken token = passwordResetToken(user());
            token.setExpiresAt(OffsetDateTime.now().minusHours(1));
            when(jwtService.hashToken("expired")).thenReturn("hash");
            when(passwordResetTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> accountService.resetPassword(new ResetPasswordRequest("expired", "NewPassword123456")))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getCode()).isEqualTo("TOKEN_EXPIRED"));
        }
    }

    @Nested
    class UpdateProfile {

        @Test
        void updatesName_whenOnlyNameProvided() {
            User user = user();
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            AuthUserResponse result = accountService.updateProfile(user.getId(), new UpdateProfileRequest("New Name", null));

            assertThat(result.name()).isEqualTo("New Name");
            assertThat(user.getName()).isEqualTo("New Name");
            verify(userRepository).save(user);
        }

        @Test
        void updatesEmail_lowercasesAndStrips_andInvalidatesSessions_andSendsVerification() {
            stubTokenGeneration();
            User user = user("old@test.com");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.findByEmailIgnoreCase("new@test.com")).thenReturn(Optional.empty());

            accountService.updateProfile(user.getId(), new UpdateProfileRequest(null, "  NEW@TEST.COM  "));
            triggerAfterCommit();

            assertThat(user.getEmail()).isEqualTo("new@test.com");
            assertThat(user.isEmailVerified()).isFalse();
            verify(refreshTokenRepository).deleteByUser(user);
            verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
            verify(emailService).sendVerificationEmail(eq("new@test.com"), anyString(), anyString());
        }

        @Test
        void updatesNameAndEmail_whenBothProvided() {
            stubTokenGeneration();
            User user = user("old@test.com");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.findByEmailIgnoreCase("new@test.com")).thenReturn(Optional.empty());

            AuthUserResponse result = accountService.updateProfile(user.getId(), new UpdateProfileRequest("Updated Name", "new@test.com"));

            assertThat(result.name()).isEqualTo("Updated Name");
            assertThat(user.getEmail()).isEqualTo("new@test.com");
        }

        @Test
        void throwsConflict_whenNewEmailAlreadyTaken() {
            User user = user("old@test.com");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.findByEmailIgnoreCase("taken@test.com")).thenReturn(Optional.of(user("taken@test.com")));

            assertThatThrownBy(() -> accountService.updateProfile(user.getId(), new UpdateProfileRequest(null, "taken@test.com")))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> {
                        AuthException ae = (AuthException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("EMAIL_TAKEN");
                    });
        }

        @Test
        void throwsBadRequest_whenNoChangesDetected() {
            User user = user("same@test.com");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> accountService.updateProfile(user.getId(), new UpdateProfileRequest(null, "same@test.com")))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> {
                        AuthException ae = (AuthException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(ae.getCode()).isEqualTo("NO_CHANGES");
                    });
        }

        @Test
        void throwsUnauthorized_whenUserNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.updateProfile(id, new UpdateProfileRequest("Name", null)))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }
    }
}
