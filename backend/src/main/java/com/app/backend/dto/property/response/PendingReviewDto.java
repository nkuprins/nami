package com.app.backend.dto.property.response;

import com.app.backend.dto.cadastre.CadastreComparison;

/**
 * One row in the admin moderation queue: the listing summary plus the
 * declared-vs-official cadastre breakdown that explains why it was held.
 */
public record PendingReviewDto(PropertyListItemDto listing, CadastreComparison cadastre) {}
