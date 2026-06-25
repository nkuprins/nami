package com.app.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreatePropertyRequest(
        @NotBlank String type,
        @NotBlank String propertyKind,
        String titleLv,
        String titleEn,
        String descriptionLv,
        String descriptionEn,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotNull @Min(0) Short rooms,
        @NotNull @DecimalMin("1.00") BigDecimal m2,
        BigDecimal landM2,
        Short floor,
        Short totalFloors,
        Short yearBuilt,
        List<String> features,
        @NotBlank String district,
        @NotBlank String city,
        @NotBlank String address,
        @NotNull @Valid CoordsDto coords,
        List<String> photos,
        String completion
) {}
