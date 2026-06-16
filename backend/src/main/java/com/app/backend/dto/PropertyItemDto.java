package com.app.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PropertyItemDto(
        String id,
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
        String postedAt,
        String completion
) {}
