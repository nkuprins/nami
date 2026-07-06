package com.app.backend.service;

import com.app.backend.dto.property.response.PropertyDto;
import com.app.backend.entity.Property;
import com.app.backend.mapper.PropertyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Read-only queries over a {@link Property}. Listing reads live in {@link ListingQueryService}. */
@Service
@RequiredArgsConstructor
public class PropertyQueryService {

    private final PropertyMapper propertyMapper;
    private final PropertyAccess propertyAccess;

    @Transactional(readOnly = true)
    public PropertyDto getProperty(UUID propertyId, UUID ownerId) {
        Property property = propertyAccess.loadOwnedProperty(propertyId, ownerId);
        return propertyMapper.toPropertyDto(property);
    }
}
