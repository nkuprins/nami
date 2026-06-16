package com.app.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PropertyItemDto(
        UUID id,
        String type,
        String propertyKind,
        String title,
        String description,
        BigDecimal price,
        Short rooms,
        BigDecimal m2,
        BigDecimal landM2,
        Short floor,
        Short totalFloors,
        Short yearBuilt,
        List<String> features,
        String district,
        String city,
        String address,
        CoordsDto coords,
        List<String> photos,
        OffsetDateTime postedAt,
        String completion
) {}
