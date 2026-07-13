package com.app.backend.service;

import com.app.backend.config.CacheConfig;
import com.app.backend.dto.property.response.PropertyListItemDto;
import com.app.backend.entity.Listing;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.exception.ApiException;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Moderation queue for listings held at {@link PropertyStatus#PENDING_REVIEW}. */
@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final ListingRepository listingRepository;
    private final PropertyMapper propertyMapper;

    @Transactional(readOnly = true)
    public List<PropertyListItemDto> listPending() {
        return listingRepository.findByStatusOrderByPostedAtAsc(PropertyStatus.PENDING_REVIEW).stream()
                .map(propertyMapper::toListDto)
                .toList();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_KIND_COUNTS, allEntries = true)
    })
    @Transactional
    public void approve(UUID listingId) {
        setStatus(listingId, PropertyStatus.ACTIVE);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.PROPERTY_KIND_COUNTS, allEntries = true)
    })
    @Transactional
    public void reject(UUID listingId) {
        setStatus(listingId, PropertyStatus.INACTIVE);
    }

    private void setStatus(UUID listingId, PropertyStatus status) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
        if (listing.getStatus() != PropertyStatus.PENDING_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "Listing is not pending review");
        }
        listing.setStatus(status);
    }
}
