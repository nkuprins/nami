package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.auth.*;
import com.app.backend.entity.*;
import com.app.backend.exception.AuthException;
import com.app.backend.repository.*;
import com.app.backend.security.CookieFactory;
import com.app.backend.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private CookieFactory cookieFactory;
    @Mock private AppProperties props;

    @InjectMocks
    private AuthService authService;

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
}
