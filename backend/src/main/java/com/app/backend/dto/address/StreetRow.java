package com.app.backend.dto.address;

/** A register street as mirrored into {@code address_streets}. */
public record StreetRow(
        long code,
        long territoryCode,
        String name,
        String normName
) {}
