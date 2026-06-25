package com.app.backend.repository;

import com.app.backend.entity.User;
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
           "(SELECT p FROM Property p WHERE p.owner = u AND p.status = com.app.backend.enums.PropertyStatus.ACTIVE)")
    List<User> findInactiveWithoutActiveListings(@Param("cutoff") OffsetDateTime cutoff);
}
