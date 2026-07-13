package com.app.backend.service;

import com.app.backend.config.CacheConfig;
import com.app.backend.dto.property.request.AddListingRequest;
import com.app.backend.dto.property.request.RenewPropertyRequest;
import com.app.backend.dto.property.request.UpdateListingRequest;
import com.app.backend.dto.property.response.PropertyItemDto;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Property;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.messaging.ImageProcessingPublisher;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Write operations scoped to a {@link Listing}. Property (address) writes live in {@link PropertyService}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;
    private final PropertyAccess propertyAccess;
    private final MediaCleanupService mediaCleanupService;
    private final MediaUrlValidator mediaUrlValidator;
    private final ImageProcessingPublisher imageProcessingPublisher;
    private final CadastreQueryService cadastreQueryService;

    /** Adds another self-contained listing at an address that already has at least one (e.g. rent one floor of a listed house). */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_KIND_COUNTS, allEntries = true)
    })
    @Transactional
    public PropertyItemDto addListing(UUID propertyId, AddListingRequest req, UUID ownerId) {
        Property property = propertyAccess.loadOwnedProperty(propertyId, ownerId);

        // Media reused from a sibling listing already has its variants generated, so only
        // genuinely-new URLs need processing — skip re-queuing the shared ones.
        Set<String> alreadyProcessed = listingRepository.findByPropertyId(propertyId).stream()
                .flatMap(l -> l.allMediaUrls().stream())
                .collect(Collectors.toSet());

        Listing listing = new Listing();
        listing.setProperty(property);
        listing.setOwner(property.getOwner());
        propertyMapper.applyListingContent(listing, req);
        mediaUrlValidator.validate(listing.allMediaUrls());
        listing.setStatus(cadastreQueryService.decideStatus(
                property.getArBuildingCode(), property.getApartment(), listing.getM2(), listing.getYearBuilt()));
        listing.setExpiresAt(OffsetDateTime.now().plusMonths(req.durationMonths()));

        Listing saved = listingRepository.save(listing);
        log.info("Listing created: {} for existing property: {} by owner: {}", saved.getId(), propertyId, ownerId);
        List<String> toProcess = saved.allMediaUrls().stream()
                .filter(u -> !alreadyProcessed.contains(u))
                .toList();
        imageProcessingPublisher.enqueue(propertyId, toProcess);
        return propertyMapper.toDto(saved);
    }

    /** Updates a self-contained listing (physical, media, features, terms). Its property's address is untouched. */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_TRANSLATION, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_KIND_COUNTS, allEntries = true)
    })
    @Transactional
    public PropertyItemDto updateListing(UUID listingId, UpdateListingRequest req, UUID ownerId) {
        Listing listing = propertyAccess.loadOwnedListing(listingId, ownerId);

        List<String> oldMedia = new ArrayList<>(listing.allMediaUrls());
        propertyMapper.applyListingContent(listing, req);
        mediaUrlValidator.validate(listing.allMediaUrls());
        // Re-derive status so a fixed area/build year can clear a hold — but never
        // resurrect a listing that's inactive for an unrelated reason (expiry, admin
        // rejection); those come back only through an explicit renew().
        if (listing.getStatus() != PropertyStatus.INACTIVE) {
            Property property = listing.getProperty();
            listing.setStatus(cadastreQueryService.decideStatus(
                    property.getArBuildingCode(), property.getApartment(), listing.getM2(), listing.getYearBuilt()));
        }
        Listing saved = listingRepository.save(listing);

        List<String> newMedia = saved.allMediaUrls();
        List<String> removed = oldMedia.stream().filter(u -> !newMedia.contains(u)).toList();
        if (!removed.isEmpty()) {
            mediaCleanupService.enqueue(removed);
        }
        List<String> added = newMedia.stream().filter(u -> !oldMedia.contains(u)).toList();
        imageProcessingPublisher.enqueue(listing.getProperty().getId(), added);

        return propertyMapper.toDto(saved);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_TRANSLATION, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_KIND_COUNTS, allEntries = true)
    })
    @Transactional
    public PropertyItemDto renew(UUID listingId, RenewPropertyRequest req, UUID ownerId) {
        Listing listing = propertyAccess.loadOwnedListing(listingId, ownerId);
        Property property = listing.getProperty();
        listing.setExpiresAt(OffsetDateTime.now().plusMonths(req.durationMonths()));
        listing.setStatus(cadastreQueryService.decideStatus(
                property.getArBuildingCode(), property.getApartment(), listing.getM2(), listing.getYearBuilt()));
        listing.setExpiryWarningSent(false);
        return propertyMapper.toDto(listingRepository.save(listing));
    }

    /**
     * Deletes a single listing and enqueues its media for cleanup. If it was the last
     * listing at the address, the (now empty) property is deleted too — an address with
     * no listings is dead. Temporary takedowns use the INACTIVE/renew lifecycle instead.
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_TRANSLATION, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_KIND_COUNTS, allEntries = true)
    })
    @Transactional
    public void deleteListing(UUID listingId, UUID ownerId) {
        Listing listing = propertyAccess.loadOwnedListing(listingId, ownerId);
        Property property = listing.getProperty();
        List<String> media = listing.allMediaUrls();
        boolean lastListing = listingRepository.countByPropertyId(property.getId()) <= 1;

        // Delete the (managed) listing before its property, otherwise the property removal would
        // leave this listing referencing a removed parent (flush would fail).
        listingRepository.delete(listing);
        if (lastListing) {
            propertyRepository.delete(property);
            log.info("Listing {} deleted with its now-empty property {} by owner: {}",
                    listingId, property.getId(), ownerId);
        } else {
            log.info("Listing {} (property {}) deleted by owner: {}", listingId, property.getId(), ownerId);
        }

        if (!media.isEmpty()) {
            mediaCleanupService.enqueue(media);
        }
    }
}
