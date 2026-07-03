package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.UserExportDto;
import com.app.backend.dto.auth.*;
import com.app.backend.entity.*;
import com.app.backend.exception.AuthException;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.repository.*;
import com.app.backend.security.CookieFactory;
import com.app.backend.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.junit.jupiter.api.AfterEach;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private SavedListingRepository savedListingRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private CookieFactory cookieFactory;
    @Mock private EmailService emailService;
    @Mock private MediaCleanupService mediaCleanupService;
    @Mock private AppProperties props;
    @Spy @SuppressWarnings("unused") private PropertyMapper propertyMapper = new PropertyMapper();

    @InjectMocks
    private AuthService authService;

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
        lenient().when(cookieFactory.accessTokenCookie(anyString())).thenReturn(ResponseCookie.from("access_token", "v").build());
        lenient().when(cookieFactory.refreshTokenCookie(anyString())).thenReturn(ResponseCookie.from("refresh_token", "v").build());
        lenient().when(cookieFactory.clearAccessToken()).thenReturn(ResponseCookie.from("access_token", "").build());
        lenient().when(cookieFactory.clearRefreshToken()).thenReturn(ResponseCookie.from("refresh_token", "").build());
        lenient().when(cookieFactory.hasSessionCookie()).thenReturn(ResponseCookie.from("has_session", "1").build());
        lenient().when(cookieFactory.clearHasSessionCookie()).thenReturn(ResponseCookie.from("has_session", "").build());
    }

    private void stubTokenGeneration() {
        lenient().when(jwtService.generateAccessToken(any())).thenReturn("access-jwt");
        lenient().when(jwtService.generateOpaqueToken()).thenReturn("opaque-token");
        lenient().when(jwtService.hashToken(anyString())).thenReturn("hashed-token");
        lenient().when(props.jwt()).thenReturn(new AppProperties.JwtProperties("secret", 900, 604800));
        lenient().when(props.frontendUrl()).thenReturn("http://localhost:3000");
    }

    @Nested
    class Register {
        @Test
        void savesUser_withLowercaseEmail_andEncodesPassword() {
            stubCookieFactory();
            stubTokenGeneration();
            when(userRepository.findByEmailIgnoreCase("Test@Example.COM")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn("$2a$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            AuthUserResponse result = authService.register(new RegisterRequest("Name", "Test@Example.COM", RAW_PASSWORD));

            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.emailVerified()).isFalse();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$encoded");
        }

        @Test
        void throwsConflict_whenEmailTaken() {
            when(userRepository.findByEmailIgnoreCase("taken@test.com")).thenReturn(Optional.of(user()));

            assertThatThrownBy(() -> authService.register(registerRequest("taken@test.com")))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> {
                        AuthException ae = (AuthException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(ae.getCode()).isEqualTo("EMAIL_TAKEN");
                    });
        }

        @Test
        void sendsVerificationEmail() {
            stubCookieFactory();
            stubTokenGeneration();
            when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            authService.register(registerRequest());
            triggerAfterCommit();

            verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
            verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        }
    }

    @Nested
    class Login {

        private HttpServletResponse response;

        @BeforeEach
        void setUp() {
            response = mock(HttpServletResponse.class);
        }

        @Test
        void returnsCookies_whenCredentialsValid() {
            stubCookieFactory();
            stubTokenGeneration();
            User user = user("login@test.com");
            when(userRepository.findByEmailIgnoreCase("login@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, user.getPasswordHash())).thenReturn(true);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AuthUserResponse result = authService.login(loginRequest("login@test.com"), response);

            assertThat(result.email()).isEqualTo("login@test.com");
            assertThat(user.getLastLoginAt()).isNotNull()
                    .isCloseToUtcNow(within(5, ChronoUnit.SECONDS));
            verify(response, times(3)).addHeader(eq("Set-Cookie"), anyString());
        }

        @Test
        void throwsUnauthorized_whenEmailNotFound() {
            when(userRepository.findByEmailIgnoreCase("none@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest("none@test.com"), response))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }

        @Test
        void throwsUnauthorized_whenPasswordWrong() {
            User user = user("wrong@test.com");
            when(userRepository.findByEmailIgnoreCase("wrong@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest("wrong@test.com"), response))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getCode()).isEqualTo("INVALID_CREDENTIALS"));
        }

        @Test
        void throwsForbidden_whenEmailNotVerified() {
            User user = unverifiedUser("unverified@test.com");
            when(userRepository.findByEmailIgnoreCase("unverified@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, user.getPasswordHash())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest("unverified@test.com"), response))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> {
                        AuthException ae = (AuthException) ex;
                        assertThat(ae.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(ae.getCode()).isEqualTo("EMAIL_NOT_VERIFIED");
                    });
        }
    }

    @Nested
    class Refresh {

        private HttpServletResponse response;

        @BeforeEach
        void setUp() {
            response = mock(HttpServletResponse.class);
        }

        @Test
        void revokesOldToken_andCreatesNewCookies() {
            stubCookieFactory();
            stubTokenGeneration();
            User user = user();
            RefreshToken token = refreshToken(user);
            when(jwtService.hashToken("raw-refresh")).thenReturn("hashed");
            when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(token));
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AuthUserResponse result = authService.refresh("raw-refresh", response);

            assertThat(result.email()).isEqualTo(user.getEmail());
            assertThat(token.isRevoked()).isTrue();
            verify(response, times(3)).addHeader(eq("Set-Cookie"), anyString());
        }

        @Test
        void throwsUnauthorized_whenTokenNull() {
            assertThatThrownBy(() -> authService.refresh(null, response))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getCode()).isEqualTo("INVALID_TOKEN"));
        }

        @Test
        void throwsUnauthorized_whenTokenBlank() {
            assertThatThrownBy(() -> authService.refresh("  ", response))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        void throwsUnauthorized_whenTokenHashNotFound() {
            when(jwtService.hashToken("unknown")).thenReturn("hash");
            when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("unknown", response))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        void throwsUnauthorized_whenTokenRevoked() {
            RefreshToken token = refreshToken(user());
            token.setRevoked(true);
            when(jwtService.hashToken("revoked")).thenReturn("hash");
            when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.refresh("revoked", response))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getCode()).isEqualTo("TOKEN_EXPIRED"));
        }

        @Test
        void throwsUnauthorized_whenTokenExpired() {
            RefreshToken token = refreshToken(user());
            token.setExpiresAt(OffsetDateTime.now().minusHours(1));
            when(jwtService.hashToken("expired")).thenReturn("hash");
            when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.refresh("expired", response))
                    .isInstanceOf(AuthException.class);
        }
    }

    @Nested
    class Logout {

        private HttpServletResponse response;

        @BeforeEach
        void setUp() {
            response = mock(HttpServletResponse.class);
            stubCookieFactory();
        }

        @Test
        void revokesToken_andClearsCookies() {
            RefreshToken token = refreshToken(user());
            when(jwtService.hashToken("raw")).thenReturn("hash");
            when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));
            when(refreshTokenRepository.save(any())).thenReturn(token);

            authService.logout("raw", response);

            assertThat(token.isRevoked()).isTrue();
            verify(response, times(3)).addHeader(eq("Set-Cookie"), anyString());
        }

        @Test
        void clearsCookies_evenWhenTokenNull() {
            authService.logout(null, response);

            verify(response, times(3)).addHeader(eq("Set-Cookie"), anyString());
            verify(refreshTokenRepository, never()).findByTokenHash(anyString());
        }

        @Test
        void clearsCookies_evenWhenTokenBlank() {
            authService.logout("  ", response);

            verify(response, times(3)).addHeader(eq("Set-Cookie"), anyString());
            verify(refreshTokenRepository, never()).findByTokenHash(anyString());
        }
    }

    @Nested
    class Me {
        @Test
        void returnsUser_whenFound() {
            User user = user();
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            AuthUserResponse result = authService.me(user.getId());

            assertThat(result.email()).isEqualTo(user.getEmail());
        }

        @Test
        void throwsUnauthorized_whenNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.me(id))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
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
            when(propertyRepository.findByOwner(user)).thenReturn(List.of(listing.getProperty()));

            authService.deleteAccount(user.getId(), response);
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

            assertThatThrownBy(() -> authService.deleteAccount(id, response))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        void doesNotCallS3_whenUserHasNoProperties() {
            User user = user();
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(propertyRepository.findByOwner(user)).thenReturn(List.of());

            authService.deleteAccount(user.getId(), response);

            verify(userRepository).delete(user);
            verify(mediaCleanupService, never()).enqueue(any());
        }

        @Test
        void cleansUpS3Photos_forPropertyWithNoListings() {
            // A property can be orphaned (all its listings deleted) yet still owned by the user;
            // it must still be picked up for S3 cleanup even though listingRepository won't return it.
            User user = user();
            Property orphaned = listingWithPhotos(user).getProperty();
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(propertyRepository.findByOwner(user)).thenReturn(List.of(orphaned));

            authService.deleteAccount(user.getId(), response);
            triggerAfterCommit();

            verify(userRepository).delete(user);
            verify(mediaCleanupService).enqueue(List.of(
                    "https://cdn.test.local/uploads/photo1.jpg",
                    "https://cdn.test.local/uploads/photo2.jpg"
            ));
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

            authService.verifyEmail(new VerifyEmailRequest("raw"));

            assertThat(token.isUsed()).isTrue();
            assertThat(user.isEmailVerified()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        void throwsBadRequest_whenTokenNotFound() {
            when(jwtService.hashToken("bad")).thenReturn("hash");
            when(emailVerificationTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("bad")))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        void throwsBadRequest_whenTokenAlreadyUsed() {
            EmailVerificationToken token = emailVerificationToken(user());
            token.setUsed(true);
            when(jwtService.hashToken("used")).thenReturn("hash");
            when(emailVerificationTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("used")))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        void throwsBadRequest_whenTokenExpired() {
            EmailVerificationToken token = emailVerificationToken(user());
            token.setExpiresAt(OffsetDateTime.now().minusHours(1));
            when(jwtService.hashToken("expired")).thenReturn("hash");
            when(emailVerificationTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("expired")))
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

            authService.resendVerification(new ResendVerificationRequest("unverified@test.com"));
            triggerAfterCommit();

            verify(emailVerificationTokenRepository).deleteByUser(user);
            verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        void doesNothing_whenAlreadyVerified() {
            User user = user("verified@test.com");
            when(userRepository.findByEmailIgnoreCase("verified@test.com")).thenReturn(Optional.of(user));

            authService.resendVerification(new ResendVerificationRequest("verified@test.com"));

            verify(emailVerificationTokenRepository, never()).deleteByUser(any());
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        void doesNothing_whenEmailNotFound() {
            when(userRepository.findByEmailIgnoreCase("ghost@test.com")).thenReturn(Optional.empty());

            assertThatCode(() -> authService.resendVerification(new ResendVerificationRequest("ghost@test.com")))
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

            authService.forgotPassword(new ForgotPasswordRequest("forgot@test.com"));
            triggerAfterCommit();

            verify(passwordResetTokenRepository).deleteByUser(user);
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            verify(emailService).sendPasswordResetEmail(eq("forgot@test.com"), anyString(), anyString());
        }

        @Test
        void doesNothing_whenEmailNotFound() {
            when(userRepository.findByEmailIgnoreCase("ghost@test.com")).thenReturn(Optional.empty());

            assertThatCode(() -> authService.forgotPassword(new ForgotPasswordRequest("ghost@test.com")))
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

            authService.resetPassword(new ResetPasswordRequest("raw", "NewPassword123456"));

            assertThat(token.isUsed()).isTrue();
            assertThat(user.getPasswordHash()).isEqualTo("$2a$new-hash");
            verify(refreshTokenRepository).deleteByUser(user);
        }

        @Test
        void throwsBadRequest_whenTokenNotFound() {
            when(jwtService.hashToken("bad")).thenReturn("hash");
            when(passwordResetTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("bad", "NewPassword123456")))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        void throwsBadRequest_whenTokenUsed() {
            PasswordResetToken token = passwordResetToken(user());
            token.setUsed(true);
            when(jwtService.hashToken("used")).thenReturn("hash");
            when(passwordResetTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("used", "NewPassword123456")))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        void throwsBadRequest_whenTokenExpired() {
            PasswordResetToken token = passwordResetToken(user());
            token.setExpiresAt(OffsetDateTime.now().minusHours(1));
            when(jwtService.hashToken("expired")).thenReturn("hash");
            when(passwordResetTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("expired", "NewPassword123456")))
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

            AuthUserResponse result = authService.updateProfile(user.getId(), new UpdateProfileRequest("New Name", null));

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

            authService.updateProfile(user.getId(), new UpdateProfileRequest(null, "  NEW@TEST.COM  "));
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

            AuthUserResponse result = authService.updateProfile(user.getId(), new UpdateProfileRequest("Updated Name", "new@test.com"));

            assertThat(result.name()).isEqualTo("Updated Name");
            assertThat(user.getEmail()).isEqualTo("new@test.com");
        }

        @Test
        void throwsConflict_whenNewEmailAlreadyTaken() {
            User user = user("old@test.com");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.findByEmailIgnoreCase("taken@test.com")).thenReturn(Optional.of(user("taken@test.com")));

            assertThatThrownBy(() -> authService.updateProfile(user.getId(), new UpdateProfileRequest(null, "taken@test.com")))
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

            assertThatThrownBy(() -> authService.updateProfile(user.getId(), new UpdateProfileRequest(null, "same@test.com")))
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

            assertThatThrownBy(() -> authService.updateProfile(id, new UpdateProfileRequest("Name", null)))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }
    }

    @Nested
    class Export {
        @Test
        void returnsDto_withOwnedAndSavedProperties() {
            User user = user();
            Listing owned = listing(user);
            UUID savedListingId = UUID.randomUUID();
            SavedListing saved = new SavedListing();
            saved.setId(new SavedListingId(user.getId(), savedListingId));
            saved.setSavedAt(OffsetDateTime.now());

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(listingRepository.findByOwner(user)).thenReturn(List.of(owned));
            when(savedListingRepository.findByIdUserId(user.getId())).thenReturn(List.of(saved));

            UserExportDto result = authService.export(user.getId());

            assertThat(result.id()).isEqualTo(user.getId());
            assertThat(result.email()).isEqualTo(user.getEmail());
            assertThat(result.ownedProperties()).hasSize(1);
            assertThat(result.savedProperties()).hasSize(1);
            assertThat(result.savedProperties().getFirst().listingId()).isEqualTo(savedListingId);
        }

        @Test
        void returnsDto_withEmptyLists_whenNoPropertiesOrSaved() {
            User user = user();
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(listingRepository.findByOwner(user)).thenReturn(List.of());
            when(savedListingRepository.findByIdUserId(user.getId())).thenReturn(List.of());

            UserExportDto result = authService.export(user.getId());

            assertThat(result.ownedProperties()).isEmpty();
            assertThat(result.savedProperties()).isEmpty();
        }

        @Test
        void throwsUnauthorized_whenUserNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.export(id))
                    .isInstanceOf(AuthException.class)
                    .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }
    }
}
