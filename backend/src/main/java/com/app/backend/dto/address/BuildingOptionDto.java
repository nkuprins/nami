package com.app.backend.dto.address;

import com.fasterxml.jackson.annotation.JsonInclude;

/** A house-number autocomplete option under a chosen street. Coordinates are WGS84, rarely absent. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildingOptionDto(
        long code,
        String name,
        Double lat,
        Double lng
) {}
