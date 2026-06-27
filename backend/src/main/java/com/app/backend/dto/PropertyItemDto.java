package com.app.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PropertyItemDto(
        UUID id,
        UUID ownerId,
        String type,
        String propertyKind,
        String titleLv,
        String titleEn,
        String titleRu,
        String descriptionLv,
        String descriptionEn,
        String descriptionRu,
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
        List<String> plans,
        List<String> phones,
        String videoUrl,
        OffsetDateTime postedAt,
        String completion,
        OffsetDateTime expiresAt
) {}
