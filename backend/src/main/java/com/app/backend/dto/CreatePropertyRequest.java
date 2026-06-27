package com.app.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
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
        String titleRu,
        String descriptionLv,
        String descriptionEn,
        String descriptionRu,
        @NotNull @DecimalMin("0.01") @DecimalMax("999999999999.99") BigDecimal price,
        @NotNull @Min(1) Short rooms,
        @NotNull @DecimalMin("1.00") @DecimalMax("9999.99") BigDecimal m2,
        @DecimalMin("0.01") @DecimalMax("999999.99") BigDecimal landM2,
        @Min(0) @Max(100) Short floor,
        @Min(1) @Max(100) Short totalFloors,
        @Min(1800) @Max(2035) Short yearBuilt,
        List<@NotBlank String> features,
        @NotBlank String district,
        @NotBlank String city,
        @NotBlank String address,
        @NotNull @Valid CoordsDto coords,
        List<@NotBlank String> photos,
        List<@NotBlank String> plans,
        List<@NotBlank String> phones,
        String videoUrl,
        String completion,
        @NotNull @Min(1) @Max(6) Integer durationMonths
) {}
