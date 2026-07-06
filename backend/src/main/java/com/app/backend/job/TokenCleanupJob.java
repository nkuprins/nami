package com.app.backend.job;

import com.app.backend.repository.EmailVerificationTokenRepository;
import com.app.backend.repository.PasswordResetTokenRepository;
import com.app.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@Profile("scheduler")
@RequiredArgsConstructor
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        OffsetDateTime now = OffsetDateTime.now();
        int refresh = refreshTokenRepository.deleteByExpiresAtBefore(now);
        int reset = passwordResetTokenRepository.deleteByExpiresAtBefore(now);
        int verification = emailVerificationTokenRepository.deleteByExpiresAtBefore(now);
        log.info("Token cleanup: deleted {} refresh, {} reset, {} verification tokens", refresh, reset, verification);
    }
}
