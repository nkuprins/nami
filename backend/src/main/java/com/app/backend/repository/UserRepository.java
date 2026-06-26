package com.app.backend.repository;

import com.app.backend.entity.User;
import com.app.backend.enums.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u WHERE u.updatedAt < :cutoff AND NOT EXISTS " +
           "(SELECT p FROM Property p WHERE p.owner = u AND p.status = :status)")
    List<User> findInactiveWithoutActiveListings(
            @Param("cutoff") OffsetDateTime cutoff,
            @Param("status") PropertyStatus status);

    @Query("SELECT u FROM User u WHERE u.updatedAt < :warnCutoff AND u.updatedAt >= :purgeCutoff " +
           "AND NOT EXISTS (SELECT p FROM Property p WHERE p.owner = u AND p.status = :status)")
    List<User> findAboutToBeInactive(
            @Param("warnCutoff") OffsetDateTime warnCutoff,
            @Param("purgeCutoff") OffsetDateTime purgeCutoff,
            @Param("status") PropertyStatus status);
}
