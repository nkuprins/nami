package com.app.backend.repository;

import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.enums.PropertyStatus;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

    @EntityGraph(attributePaths = {"owner"})
    List<Property> findByOwner(User owner);

    @Override
    @EntityGraph(attributePaths = {"owner"})
    Page<Property> findAll(Specification<Property> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"owner", "photos", "plans", "phones", "features", "translations"})
    Optional<Property> findById(UUID id);

    @Query("SELECT p FROM Property p WHERE p.status = :status AND p.expiresAt <= :cutoff")
    List<Property> findExpired(@Param("status") PropertyStatus status, @Param("cutoff") OffsetDateTime cutoff);

    @Query("SELECT p FROM Property p WHERE p.status = :status AND p.expiresAt <= :warnCutoff AND p.expiryWarningSent = false")
    List<Property> findExpiringUnwarned(@Param("status") PropertyStatus status, @Param("warnCutoff") OffsetDateTime warnCutoff);
}
