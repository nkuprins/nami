package com.app.backend.dto.property.response;

/** Active-listing counts per {@code property_category} for a given transaction type. */
public record PropertyCategoryCountsDto(
        long apartment,
        long house,
        long newProject,
        long commercial,
        long land,
        long garage
) {}
