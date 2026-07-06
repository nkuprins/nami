package com.app.backend.service;

import com.app.backend.config.CacheConfig;
import com.app.backend.dto.property.request.AddListingRequest;
import com.app.backend.dto.property.request.RenewPropertyRequest;
import com.app.backend.dto.property.request.UpdateListingRequest;
import com.app.backend.dto.property.response.PropertyItemDto;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Property;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.exception.ApiException;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Write operations scoped to a {@link Listing}. Property writes live in {@link PropertyService}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final PropertyMapper propertyMapper;
    private final PropertyAccess propertyAccess;

    /** Adds another listing (e.g. rent) to a property that already has at least one other listing (e.g. buy). */
    @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true)
    @Transactional
    public PropertyItemDto addListing(UUID propertyId, AddListingRequest req, UUID ownerId) {
        Property property = propertyAccess.loadOwnedProperty(propertyId, ownerId);
        if (listingRepository.existsByPropertyIdAndListingType(propertyId, req.type())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "A " + req.type().getDbValue() + " listing already exists for this property");
        }
        validateCompletion(req.completion(), req.type(), property.getYearBuilt());

        Listing listing = new Listing();
        listing.setProperty(property);
        listing.setOwner(property.getOwner());
        listing.setListingType(req.type());
        listing.setPrice(req.price().amount());
        listing.setVatIncluded(Boolean.TRUE.equals(req.price().vatIncluded()));
        listing.setCompletion(req.completion());
        propertyMapper.applyTranslations(listing, req.translations());
        listing.setPhones(nullToEmpty(req.phones()));
        listing.setStatus(PropertyStatus.ACTIVE);
        listing.setExpiresAt(OffsetDateTime.now().plusMonths(req.durationMonths()));

        Listing saved = listingRepository.save(listing);
        log.info("Listing created: {} for existing property: {} by owner: {}", saved.getId(), propertyId, ownerId);
        return propertyMapper.toDto(saved);
    }

    /** Updates only a listing's own fields (price, translations, phones, completion). The property is untouched. */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, key = "#listingId")
    })
    @Transactional
    public PropertyItemDto updateListing(UUID listingId, UpdateListingRequest req, UUID ownerId) {
        Listing listing = propertyAccess.loadOwnedListing(listingId, ownerId);
        validateCompletion(req.completion(), req.type(), listing.getProperty().getYearBuilt());

        propertyMapper.applyListingFields(listing, req);

        return propertyMapper.toDto(listingRepository.save(listing));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, key = "#listingId")
    })
    @Transactional
    public PropertyItemDto renew(UUID listingId, RenewPropertyRequest req, UUID ownerId) {
        Listing listing = propertyAccess.loadOwnedListing(listingId, ownerId);
        listing.setExpiresAt(OffsetDateTime.now().plusMonths(req.durationMonths()));
        listing.setStatus(PropertyStatus.ACTIVE);
        listing.setExpiryWarningSent(false);
        return propertyMapper.toDto(listingRepository.save(listing));
    }

    /** Deletes a single listing. The property (and any other listings on it) is left intact. */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, key = "#listingId")
    })
    @Transactional
    public void deleteListing(UUID listingId, UUID ownerId) {
        Listing listing = propertyAccess.loadOwnedListing(listingId, ownerId);

        listingRepository.delete(listing);
        log.info("Listing {} (property {}) deleted by owner: {}", listingId, listing.getProperty().getId(), ownerId);
    }

    private static void validateCompletion(PropertyCompletion completion, ListingType type,
                                            Short yearBuilt) {
        if (completion == PropertyCompletion.NOT_READY && yearBuilt != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A 'not_ready' new project cannot have a year_built");
        }
        if (completion != null && type != ListingType.NEW_PROJECT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "completion is only valid for a new_project listing");
        }
    }

    private static List<String> nullToEmpty(List<String> values) {
        return values != null ? new ArrayList<>(values) : new ArrayList<>();
    }
}
