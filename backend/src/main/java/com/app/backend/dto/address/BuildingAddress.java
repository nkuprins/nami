package com.app.backend.dto.address;

/**
 * A register building resolved for property creation: everything needed to
 * derive the canonical address text. {@code streetName} is null for rural
 * houses named directly under their territory.
 */
public record BuildingAddress(
        long code,
        String houseName,
        String streetName,
        String territoryName,
        Double lat,
        Double lng
) {}
