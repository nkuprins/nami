package com.app.backend.dto.address;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A street-field autocomplete option. {@code kind} is {@code street} for a
 * register street (pick a house number next) or {@code house} for a rural house
 * named directly under its territory ({@code code} is then a building code and
 * the coordinates are present — the selection is terminal).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StreetOptionDto(
        String kind,
        long code,
        String name,
        String territory,
        Double lat,
        Double lng
) {
    public static StreetOptionDto street(long code, String name, String territory) {
        return new StreetOptionDto("street", code, name, territory, null, null);
    }

    public static StreetOptionDto house(long code, String name, String territory, Double lat, Double lng) {
        return new StreetOptionDto("house", code, name, territory, lat, lng);
    }
}
