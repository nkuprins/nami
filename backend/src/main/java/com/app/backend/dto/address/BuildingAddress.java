package com.app.backend.dto.address;

/**
 * A register building resolved for property creation: everything needed to
 * derive the canonical address text. {@code streetName} / {@code streetCode}
 * are null for rural houses named directly under their territory.
 */
public record BuildingAddress(
        long code,
        String houseName,
        String streetName,
        Long streetCode,
        String territoryName,
        Double lat,
        Double lng
) {}
