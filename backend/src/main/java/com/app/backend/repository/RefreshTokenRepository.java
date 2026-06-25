package com.app.backend.repository;

import com.app.backend.entity.RefreshToken;
import com.app.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteByUser(User user);
    int deleteByExpiresAtBefore(OffsetDateTime cutoff);
}
