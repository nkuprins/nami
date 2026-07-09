package com.app.backend.repository;

import com.app.backend.entity.Listing;
import com.app.backend.entity.User;
import com.app.backend.enums.PropertyStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing>, ListingRepositoryCustom {

    @EntityGraph(attributePaths = {"owner", "property", "features", "translations"})
    List<Listing> findByOwner(User owner);

    @Override
    @EntityGraph(attributePaths = {"owner", "property", "features", "translations", "phones"})
    Optional<Listing> findById(UUID id);

    @Query("SELECT l FROM Listing l WHERE l.status = :status AND l.expiresAt <= :cutoff")
    List<Listing> findExpired(@Param("status") PropertyStatus status, @Param("cutoff") OffsetDateTime cutoff);

    @Query("SELECT l FROM Listing l WHERE l.status = :status AND l.expiresAt <= :warnCutoff AND l.expiryWarningSent = false")
    List<Listing> findExpiringUnwarned(@Param("status") PropertyStatus status, @Param("warnCutoff") OffsetDateTime warnCutoff);

    @Query("SELECT l FROM Listing l WHERE l.status = :status AND l.expiresAt < :purgeCutoff")
    List<Listing> findInactiveExpiredBefore(@Param("status") PropertyStatus status, @Param("purgeCutoff") OffsetDateTime purgeCutoff);

    @Query("SELECT COUNT(l) FROM Listing l WHERE l.property.id = :propertyId")
    long countByPropertyId(@Param("propertyId") UUID propertyId);

    List<Listing> findByPropertyId(UUID propertyId);
}
