package com.app.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreatePropertyRequest(
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
        String completion
) {}
