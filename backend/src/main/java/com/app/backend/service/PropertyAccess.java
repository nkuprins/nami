package com.app.backend.service;

import com.app.backend.entity.Listing;
import com.app.backend.entity.Property;
import com.app.backend.exception.ApiException;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Loads a property or listing and asserts the caller owns it, throwing
 * {@code NOT_FOUND} / {@code FORBIDDEN} otherwise. Shared by the query and
 * command services so the ownership check lives in exactly one place.
 */
@Service
@RequiredArgsConstructor
public class PropertyAccess {

    private final PropertyRepository propertyRepository;
    private final ListingRepository listingRepository;

    Property loadOwnedProperty(UUID propertyId, UUID ownerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN);
        }
        return property;
    }

    Listing loadOwnedListing(UUID listingId, UUID ownerId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
        if (!listing.getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN);
        }
        return listing;
    }
}
