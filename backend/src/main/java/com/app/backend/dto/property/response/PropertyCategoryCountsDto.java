package com.app.backend.dto.property.response;

import lombok.Builder;

/** Active-listing counts per {@code property_category} for a given transaction type. */
@Builder
public record PropertyCategoryCountsDto(
        long apartment,
        long house,
        long newProject,
        long commercial,
        long land,
        long garage
) {}
