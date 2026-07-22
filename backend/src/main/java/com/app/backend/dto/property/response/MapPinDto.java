package com.app.backend.dto.property.response;

import java.util.List;
import java.util.UUID;

/**
 * One map pin = one physical {@link com.app.backend.entity.Property} (an address). It carries every
 * matching listing at that address (grouped from the same filtered result set as the list), cheapest
 * first — so the popup can show all of them and never hides a listing the filter kept.
 */
public record MapPinDto(
        UUID propertyId,
        double lat,
        double lng,
        List<PropertyListItemDto> listings
) {}
