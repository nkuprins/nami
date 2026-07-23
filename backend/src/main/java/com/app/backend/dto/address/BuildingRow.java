package com.app.backend.dto.address;

import lombok.Builder;

/**
 * A register building as mirrored into {@code address_buildings}. The name is a
 * house number ("12", "12 k-1") when the building sits on a street, or a rural
 * house name when {@code streetCode} is null. Coordinates are WGS84.
 */
@Builder
public record BuildingRow(
        long code,
        Long streetCode,
        long territoryCode,
        String name,
        String normName,
        Double lat,
        Double lng
) {}
