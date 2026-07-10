package com.app.backend.service;

import com.app.backend.config.CacheConfig;
import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.request.PropertyFilter;
import com.app.backend.dto.property.response.PropertyCategoryCountsDto;
import com.app.backend.dto.property.response.PropertyItemDto;
import com.app.backend.dto.property.response.PropertyListItemDto;
import com.app.backend.dto.property.response.PropertyPageResponse;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Listing_;
import com.app.backend.entity.User;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.exception.ApiException;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.spec.PropertySearchCriteria;
import com.app.backend.spec.PropertySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read-only queries over {@link Listing}s. Property reads live in {@link PropertyQueryService}. */
@Service
@RequiredArgsConstructor
public class ListingQueryService {

    private static final int PAGE_SIZE = 12;

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final PropertyMapper propertyMapper;

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

    @Cacheable(cacheNames = CacheConfig.PROPERTY_KIND_COUNTS, key = "#type")
    @Transactional(readOnly = true)
    public PropertyCategoryCountsDto countsByType(ListingType type) {
        long apartment = listingRepository.countByListingTypeAndPropertyCategoryAndStatus(
                type, PropertyCategory.APARTMENT, PropertyStatus.ACTIVE);
        long house = listingRepository.countByListingTypeAndPropertyCategoryAndStatus(
                type, PropertyCategory.HOUSE, PropertyStatus.ACTIVE);
        return new PropertyCategoryCountsDto(apartment, house);
    }

    @Cacheable(cacheNames = CacheConfig.PROPERTY_DETAIL, key = "{#id, #locale}")
    @Transactional(readOnly = true)
    public PropertyItemDto getById(UUID id, String locale) {
        return listingRepository.findById(id)
                .filter(l -> l.getStatus() == PropertyStatus.ACTIVE)
                .map(l -> propertyMapper.toDto(l, locale))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
    }

    @Cacheable(cacheNames = CacheConfig.PROPERTY_TRANSLATION, key = "{#id, #locale}")
    @Transactional(readOnly = true)
    public LocalizedText getTranslation(UUID id, String locale) {
        return listingRepository.findById(id)
                .filter(l -> l.getStatus() == PropertyStatus.ACTIVE)
                .map(l -> propertyMapper.toTranslation(l, locale))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
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
