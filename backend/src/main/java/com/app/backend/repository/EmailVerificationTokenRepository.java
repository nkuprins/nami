package com.app.backend.repository;

import com.app.backend.entity.EmailVerificationToken;
import com.app.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    void deleteByUser(User user);
    int deleteByExpiresAtBefore(OffsetDateTime cutoff);
}
