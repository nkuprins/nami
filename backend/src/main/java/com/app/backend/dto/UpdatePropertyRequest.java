package com.app.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record UpdatePropertyRequest(
        @NotBlank String type,
        @NotBlank String propertyKind,
        String titleLv,
        String titleEn,
        String titleRu,
        String descriptionLv,
        String descriptionEn,
        String descriptionRu,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotNull @Min(0) Short rooms,
        @NotNull @DecimalMin("1.00") BigDecimal m2,
        @DecimalMin("0.01") BigDecimal landM2,
        @Min(-10) @Max(200) Short floor,
        @Min(1) @Max(200) Short totalFloors,
        @Min(1800) @Max(2100) Short yearBuilt,
        List<@NotBlank String> features,
        List<@NotBlank String> phones,
        String videoUrl,
        String completion
) {}
