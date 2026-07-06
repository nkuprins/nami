package com.app.backend.service;

import com.app.backend.dto.property.request.CreatePropertyRequest;
import com.app.backend.dto.property.model.Location;
import com.app.backend.dto.property.response.PropertyDto;
import com.app.backend.dto.property.response.PropertyItemDto;
import com.app.backend.dto.property.request.UpdatePropertyRequest;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.messaging.ImageProcessingPublisher;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Write operations scoped to a {@link Property}. Listing writes live in {@link ListingService}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {

    private static final long MAX_PROPERTIES_PER_USER = 20;

    private final PropertyRepository propertyRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final PropertyMapper propertyMapper;
    private final MediaCleanupService mediaCleanupService;
    private final ImageProcessingPublisher imageProcessingPublisher;
    private final PropertyAccess propertyAccess;

    @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true)
    @Transactional
    public PropertyItemDto create(CreatePropertyRequest req, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        checkPropertyLimit(owner);
        checkNoDuplicateProperty(owner, req.location(), Boolean.TRUE.equals(req.confirmedDuplicate()), null);

        Property property = new Property();
        property.setOwner(owner);
        Listing listing = new Listing();
        listing.setProperty(property);
        listing.setOwner(owner);

        propertyMapper.applyCommon(listing, property, req);

        Location location = req.location();
        property.setDistrictSlug(location.district());
        property.setCitySlug(location.city());
        property.setAddress(location.address());
        property.setLat(location.coords().lat());
        property.setLng(location.coords().lng());

        listing.setStatus(PropertyStatus.ACTIVE);
        listing.setExpiresAt(OffsetDateTime.now().plusMonths(req.durationMonths()));

        propertyRepository.save(property);
        Listing saved = listingRepository.save(listing);
        log.info("Listing created: {} for property: {} by owner: {}", saved.getId(), property.getId(), ownerId);
        imageProcessingPublisher.enqueue(property.getId(), property.allMediaUrls());
        return propertyMapper.toDto(saved);
    }

    /** Updates only a property's own fields (details, media, features, location). Its listings are untouched. */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, allEntries = true)
    })
    @Transactional
    public PropertyDto updateProperty(UUID propertyId, UpdatePropertyRequest req, UUID ownerId) {
        Property property = propertyAccess.loadOwnedProperty(propertyId, ownerId);

        checkNoDuplicateProperty(property.getOwner(), req.location(), true, propertyId);

        List<String> oldMedia = new ArrayList<>(property.allMediaUrls());

        propertyMapper.applyPropertyFields(property, req);
        PropertyDto dto = propertyMapper.toPropertyDto(propertyRepository.save(property));

        List<String> newMedia = property.allMediaUrls();
        List<String> removed = oldMedia.stream().filter(u -> !newMedia.contains(u)).toList();
        if (!removed.isEmpty()) {
            mediaCleanupService.enqueue(removed);
        }

        List<String> added = newMedia.stream().filter(u -> !oldMedia.contains(u)).toList();
        imageProcessingPublisher.enqueue(property.getId(), added);
        return dto;
    }

    /** Re-queues every photo and plan of a property for variant generation — a recovery hatch for a lost publish. */
    @Transactional(readOnly = true)
    public void reprocessImages(UUID propertyId, UUID ownerId) {
        Property property = propertyAccess.loadOwnedProperty(propertyId, ownerId);
        imageProcessingPublisher.enqueue(property.getId(), property.allMediaUrls());
    }

    /** Deletes a property and, via cascade, every listing on it. */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, allEntries = true)
    })
    @Transactional
    public void deleteProperty(UUID propertyId, UUID ownerId) {
        Property property = propertyAccess.loadOwnedProperty(propertyId, ownerId);

        List<String> allMediaUrls = property.allMediaUrls();

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
     * Rejects creating (or editing into) a second property at a location the owner
     * already has. An exact address match is always blocked; a near match (likely
     * typo or disguised copy) is blocked unless the caller has confirmed it is a
     * genuinely different property.
     */
    private void checkNoDuplicateProperty(User owner, Location location, boolean confirmedDuplicate,
                                          UUID excludeId) {
        for (Property existing : propertyRepository.findByOwner(owner)) {
            if (existing.getId().equals(excludeId)
                    || !existing.getCitySlug().equals(location.city())
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
}
