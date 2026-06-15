package com.app.backend.repository;

import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.enums.ListingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    List<Property> findByOwner(User owner);
    List<Property> findByCitySlug(String citySlug);
    List<Property> findByCitySlugAndListingType(String citySlug, ListingType listingType);
    Page<Property> findByCitySlugAndListingTypeAndPriceBetween(
            String citySlug, ListingType listingType,
            BigDecimal minPrice, BigDecimal maxPrice,
            Pageable pageable);
}
