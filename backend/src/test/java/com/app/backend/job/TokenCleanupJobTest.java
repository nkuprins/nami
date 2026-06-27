package com.app.backend.job;

import com.app.backend.repository.EmailVerificationTokenRepository;
import com.app.backend.repository.PasswordResetTokenRepository;
import com.app.backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenCleanupJobTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @InjectMocks
    private TokenCleanupJob tokenCleanupJob;

    @Test
    void purgeExpiredTokens_deletesFromAllRepositories() {
        when(refreshTokenRepository.deleteByExpiresAtBefore(any())).thenReturn(3);
        when(passwordResetTokenRepository.deleteByExpiresAtBefore(any())).thenReturn(1);
        when(emailVerificationTokenRepository.deleteByExpiresAtBefore(any())).thenReturn(2);

        tokenCleanupJob.purgeExpiredTokens();

        ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(refreshTokenRepository).deleteByExpiresAtBefore(captor.capture());
        verify(passwordResetTokenRepository).deleteByExpiresAtBefore(any());
        verify(emailVerificationTokenRepository).deleteByExpiresAtBefore(any());

        assertThat(captor.getValue()).isBefore(OffsetDateTime.now().plusSeconds(5));
        assertThat(captor.getValue()).isAfter(OffsetDateTime.now().minusSeconds(5));
    }
}
