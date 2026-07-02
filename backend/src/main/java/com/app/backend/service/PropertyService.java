package com.app.backend.service;

import com.app.backend.dto.AddListingRequest;
import com.app.backend.dto.CreatePropertyRequest;
import com.app.backend.dto.Location;
import com.app.backend.dto.PropertyDto;
import com.app.backend.dto.PropertyFilter;
import com.app.backend.dto.PropertyItemDto;
import com.app.backend.dto.PropertyListItemDto;
import com.app.backend.dto.PropertyPageResponse;
import com.app.backend.dto.RenewPropertyRequest;
import com.app.backend.dto.UpdateListingRequest;
import com.app.backend.dto.UpdatePropertyRequest;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Listing_;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.enums.*;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.spec.PropertySearchCriteria;
import com.app.backend.spec.PropertySpec;
import com.app.backend.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.app.backend.exception.ApiException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {

    private static final int PAGE_SIZE = 12;

    private final ListingRepository listingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyMapper propertyMapper;
    private final UploadService uploadService;

    @Cacheable(cacheNames = CacheConfig.PROPERTY_LIST, key = "{#filter, #sort, #page}")
    @Transactional(readOnly = true)
    public PropertyPageResponse list(PropertyFilter filter, String sort, int page) {
        PropertySearchCriteria criteria = PropertySearchCriteria.from(filter, parseLocFilter(filter.loc()));
        Specification<Listing> spec = PropertySpec.build(criteria);

        PageRequest pageRequest = PageRequest.of(page - 1, PAGE_SIZE, buildSort(sort));
        Page<PropertyListItemDto> result = listingRepository.findAllForList(spec, pageRequest);
        return new PropertyPageResponse(result.getContent(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<PropertyListItemDto> listByOwner(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return listingRepository.findByOwner(owner).stream()
                .map(propertyMapper::toListDto)
                .toList();
    }

    @Cacheable(cacheNames = CacheConfig.PROPERTY_DETAIL, key = "#id")
    @Transactional(readOnly = true)
    public PropertyItemDto getById(UUID id) {
        return listingRepository.findById(id)
                .filter(l -> l.getStatus() == PropertyStatus.ACTIVE)
                .map(propertyMapper::toDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
    }

    @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true)
    @Transactional
    public PropertyItemDto create(CreatePropertyRequest req, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

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
        return propertyMapper.toDto(saved);
    }

    /** Adds another listing (e.g. rent) to a property that already has at least one other listing (e.g. buy). */
    @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true)
    @Transactional
    public PropertyItemDto addListing(UUID propertyId, AddListingRequest req, UUID ownerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN);
        }
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
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
        if (!listing.getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN);
        }
        validateCompletion(req.completion(), req.type(), listing.getProperty().getYearBuilt());

        propertyMapper.applyListingFields(listing, req);

        return propertyMapper.toDto(listingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public PropertyDto getProperty(UUID propertyId, UUID ownerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN);
        }
        return propertyMapper.toPropertyDto(property);
    }

    /** Updates only a property's own fields (details, media, features, location). Its listings are untouched. */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, allEntries = true)
    })
    @Transactional
    public PropertyDto updateProperty(UUID propertyId, UpdatePropertyRequest req, UUID ownerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN);
        }

        propertyMapper.applyPropertyFields(property, req);

        return propertyMapper.toPropertyDto(propertyRepository.save(property));
    }

    private static void validateCompletion(PropertyCompletion completion, ListingType type,
                                            @Nullable Short yearBuilt) {
        if (completion == PropertyCompletion.NOT_READY && yearBuilt != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A 'not_ready' new project cannot have a year_built");
        }
        if (completion != null && type != ListingType.NEW_PROJECT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "completion is only valid for a new_project listing");
        }
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, key = "#listingId")
    })
    @Transactional
    public PropertyItemDto renew(UUID listingId, RenewPropertyRequest req, UUID ownerId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
        if (!listing.getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN);
        }
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
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
        if (!listing.getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN);
        }

        listingRepository.delete(listing);
        log.info("Listing {} (property {}) deleted by owner: {}", listingId, listing.getProperty().getId(), ownerId);
    }

    /** Deletes a property and, via cascade, every listing on it. */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, allEntries = true)
    })
    @Transactional
    public void deleteProperty(UUID propertyId, UUID ownerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN);
        }

        List<String> allMediaUrls = property.allMediaUrls();

        propertyRepository.delete(property);
        log.info("Property {} (and all its listings) deleted by owner: {}", propertyId, ownerId);

        if (!allMediaUrls.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        uploadService.deleteObjects(allMediaUrls);
                    } catch (Exception e) {
                        log.warn("Failed to delete S3 objects for property {}: {}", propertyId, e.getMessage());
                    }
                }
            });
        }
    }

    /** Parses {@code city:district} filter entries; rejects malformed ones rather than silently dropping. */
    private static Map<String, List<String>> parseLocFilter(List<String> loc) {
        if (loc == null || loc.isEmpty()) return Map.of();
        Map<String, List<String>> byCity = new HashMap<>();
        for (String entry : loc) {
            int colon = entry.indexOf(':');
            if (colon <= 0 || colon >= entry.length() - 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Malformed loc filter entry: " + entry);
            }
            byCity.computeIfAbsent(entry.substring(0, colon), k -> new ArrayList<>())
                    .add(entry.substring(colon + 1));
        }
        return byCity;
    }

    private static List<String> nullToEmpty(List<String> values) {
        return values != null ? new ArrayList<>(values) : new ArrayList<>();
    }

    private static Sort buildSort(String sort) {
        return switch (sort) {
            case "newest" -> Sort.by(Sort.Direction.DESC, Listing_.POSTED_AT);
            case "price-asc" -> Sort.by(Sort.Direction.ASC, Listing_.PRICE);
            case "price-desc" -> Sort.by(Sort.Direction.DESC, Listing_.PRICE);
            case "m2-desc" -> Sort.by(Sort.Direction.DESC, "m2");
            case "price-per-m2-asc" -> Sort.by(Sort.Direction.ASC, "pricePerM2");
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown sort option: " + sort);
        };
    }
}
