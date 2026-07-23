package com.app.backend.dto.address;

import lombok.Builder;

/**
 * A register territory (city, parish or village) as mirrored into
 * {@code address_territories}. {@code novadsName} is the resolved municipality,
 * null for republic cities that hang directly under the country.
 */
@Builder
public record TerritoryRow(
        long code,
        int typeCd,
        String name,
        String normName,
        String novadsName,
        String normNovadsName
) {}
