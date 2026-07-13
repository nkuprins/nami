package com.app.backend.service;

import com.app.backend.dto.property.request.CreatePropertyRequest;
import com.app.backend.dto.property.model.Location;
import com.app.backend.dto.property.response.PropertyDto;
import com.app.backend.dto.property.response.PropertyItemDto;
import com.app.backend.dto.property.request.UpdatePropertyRequest;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.dto.address.BuildingAddress;
import com.app.backend.messaging.ImageProcessingPublisher;
import com.app.backend.repository.AddressRegistryRepository;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.validation.AddressMatcher;
import com.app.backend.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.backend.exception.ApiException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Write operations scoped to a {@link Property} (the shared address). Listing writes live in {@link ListingService}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {

    private static final long MAX_PROPERTIES_PER_USER = 20;

    private final PropertyRepository propertyRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final AddressRegistryRepository addressRegistryRepository;
    private final PropertyMapper propertyMapper;
    private final MediaCleanupService mediaCleanupService;
    private final MediaUrlValidator mediaUrlValidator;
    private final ImageProcessingPublisher imageProcessingPublisher;
    private final PropertyAccess propertyAccess;
    private final CadastreQueryService cadastreQueryService;

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_KIND_COUNTS, allEntries = true)
    })
    @Transactional
    public PropertyItemDto create(CreatePropertyRequest req, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        checkPropertyLimit(owner);
        Location location = resolveLocation(req.location());
        checkNoDuplicateProperty(owner, location, Boolean.TRUE.equals(req.confirmedDuplicate()), null);

        Property property = new Property();
        property.setOwner(owner);
        propertyMapper.applyPropertyLocation(property, location);

        Listing listing = new Listing();
        listing.setProperty(property);
        listing.setOwner(owner);
        propertyMapper.applyListingContent(listing, req);
        mediaUrlValidator.validate(listing.allMediaUrls());
        listing.setStatus(cadastreQueryService.decideStatus(
                location.arBuildingCode(), location.apartment(), listing.getM2(), listing.getYearBuilt()));
        listing.setExpiresAt(OffsetDateTime.now().plusMonths(req.durationMonths()));

        propertyRepository.save(property);
        Listing saved = listingRepository.save(listing);
        log.info("Listing created: {} for property: {} by owner: {}", saved.getId(), property.getId(), ownerId);
        imageProcessingPublisher.enqueue(property.getId(), saved.allMediaUrls());
        return propertyMapper.toDto(saved);
    }

    /** Updates a property's shared address (location). Its listings' physical/media attributes are untouched. */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_TRANSLATION, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_KIND_COUNTS, allEntries = true)
    })
    @Transactional
    public PropertyDto updateProperty(UUID propertyId, UpdatePropertyRequest req, UUID ownerId) {
        Property property = propertyAccess.loadOwnedProperty(propertyId, ownerId);

        Location location = resolveLocation(req.location());
        checkNoDuplicateProperty(property.getOwner(), location, true, propertyId);

        propertyMapper.applyPropertyLocation(property, location);
        return propertyMapper.toPropertyDto(propertyRepository.save(property));
    }

    /** Re-queues every photo and plan of a listing for variant generation — a recovery hatch for a lost publish. */
    @Transactional(readOnly = true)
    public void reprocessImages(UUID listingId, UUID ownerId) {
        Listing listing = propertyAccess.loadOwnedListing(listingId, ownerId);
        imageProcessingPublisher.enqueue(listing.getProperty().getId(), listing.allMediaUrls());
    }

    /** Deletes a property and, via cascade, every listing on it. */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_TRANSLATION, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_KIND_COUNTS, allEntries = true)
    })
    @Transactional
    public void deleteProperty(UUID propertyId, UUID ownerId) {
        Property property = propertyAccess.loadOwnedProperty(propertyId, ownerId);

        List<Listing> listings = listingRepository.findByPropertyId(propertyId);
        List<String> allMediaUrls = listings.stream()
                .flatMap(l -> l.allMediaUrls().stream())
                .toList();

        // Delete the (managed) listings before their property: they're already loaded here, and a
        // property removal alone would leave them referencing a removed parent (flush would fail).
        listingRepository.deleteAll(listings);
        propertyRepository.delete(property);
        log.info("Property {} (and all its listings) deleted by owner: {}", propertyId, ownerId);

        if (!allMediaUrls.isEmpty()) {
            mediaCleanupService.enqueue(allMediaUrls);
        }
    }

    private void checkPropertyLimit(User owner) {
        if (propertyRepository.countByOwner(owner) >= MAX_PROPERTIES_PER_USER) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "You have reached the maximum number of properties (" + MAX_PROPERTIES_PER_USER + ")");
        }
    }

    /**
     * Anchors a register-linked location to the State Address Register: verifies
     * the building code and derives the canonical display address from the
     * register row plus the free-typed apartment. Legacy free-text locations
     * (no building code) pass through unchanged.
     */
    private Location resolveLocation(Location location) {
        if (location.arBuildingCode() == null) {
            return location;
        }
        BuildingAddress building = addressRegistryRepository.findBuildingAddress(location.arBuildingCode())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Unknown address register building code"));
        String base = building.streetName() != null
                ? building.streetName() + " " + building.houseName()
                : "\"" + building.houseName() + "\"";
        String apartment = trimToNull(location.apartment());
        return location.toBuilder()
                .address(apartment != null ? base + " - " + apartment : base)
                .apartment(apartment)
                .build();
    }

    /**
     * Rejects creating (or editing into) a second property at a location the owner
     * already has. Register-linked addresses compare exactly on building + apartment
     * for a hard duplicate; within the same building, an apartment string that's a
     * near match (likely typo, e.g. "5" vs "5a") is blocked unless the caller has
     * confirmed it is a genuinely different apartment — a different building, or a
     * clearly different apartment number, is legitimately different. Legacy
     * free-text addresses keep the same fuzzy match applied to the whole address:
     * exact is always blocked, a near match is blocked unless confirmed.
     */
    private void checkNoDuplicateProperty(User owner, Location location, boolean confirmedDuplicate,
                                          UUID excludeId) {
        String apartment = normalizedApartment(location.apartment());
        for (Property existing : propertyRepository.findByOwner(owner)) {
            if (existing.getId().equals(excludeId)) {
                continue;
            }
            if (location.arBuildingCode() != null && existing.getArBuildingCode() != null) {
                if (location.arBuildingCode().equals(existing.getArBuildingCode())) {
                    String existingApartment = normalizedApartment(existing.getApartment());
                    if (apartment.equals(existingApartment)) {
                        throw new ApiException(HttpStatus.CONFLICT, "You already have a property at this address");
                    }
                    if (!confirmedDuplicate && AddressMatcher.isNearMatch(existingApartment, apartment)) {
                        throw new ApiException(HttpStatus.CONFLICT,
                                "NEAR_DUPLICATE: this looks very similar to a property you already have");
                    }
                }
                continue;
            }
            // Legacy path: at least one side is free-text — fall back to fuzzy matching.
            if (!existing.getCitySlug().equals(location.city())
                    || !existing.getDistrictSlug().equals(location.district())) {
                continue;
            }
            if (AddressMatcher.isDuplicate(existing.getAddress(), location.address())) {
                throw new ApiException(HttpStatus.CONFLICT, "You already have a property at this address");
            }
            if (!confirmedDuplicate && AddressMatcher.isNearMatch(existing.getAddress(), location.address())) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "NEAR_DUPLICATE: this looks very similar to a property you already have");
            }
        }
    }

    /** Apartment for comparison: normalized so "4", " 4", "dz. 4" and "apt 4" collide. Empty when absent. */
    private static String normalizedApartment(String apartment) {
        if (apartment == null) {
            return "";
        }
        String normalized = AddressMatcher.normalize(apartment);
        for (String unitWord : List.of("dz ", "dzivoklis ", "apt ", "apartment ", "kv ")) {
            if (normalized.startsWith(unitWord)) {
                return normalized.substring(unitWord.length());
            }
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
