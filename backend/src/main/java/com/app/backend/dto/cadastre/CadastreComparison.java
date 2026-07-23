package com.app.backend.dto.cadastre;

import com.app.backend.enums.LandUse;

import java.math.BigDecimal;

/**
 * Declared-vs-official breakdown for a held listing, shown in the admin review
 * queue. Each {@code official*} is null when the mirror has no record to compare
 * against; each {@code *Mismatch} flag is true only when both sides were present
 * and disagreed (the same condition that held the listing).
 */
public record CadastreComparison(
        Short declaredYear, Short officialYear, boolean yearMismatch,
        BigDecimal declaredArea, BigDecimal officialArea, boolean areaMismatch,
        BigDecimal declaredLandM2, BigDecimal officialLandM2, boolean landAreaMismatch,
        LandUse declaredLandUse, LandUse officialLandUse, boolean landUseMismatch
) {}
