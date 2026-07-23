package com.app.backend.service;

import com.app.backend.dto.cadastre.CadastreComparison;
import com.app.backend.dto.cadastre.CadastreDecision;
import com.app.backend.dto.cadastre.CadastreParcelRow;
import com.app.backend.dto.cadastre.OfficialBuilding;
import com.app.backend.dto.cadastre.OfficialParcel;
import com.app.backend.entity.Listing;
import com.app.backend.enums.LandUse;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.repository.AddressRegistryRepository;
import com.app.backend.repository.CadastreRepository;
import com.app.backend.validation.AddressMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Cross-checks a listing's posted area, build year, plot area and land-use
 * against the VZD cadastre mirror. A single {@link #snapshot} gathers the
 * official-vs-declared figures once; three consumers derive from it:
 * {@link #decideStatus} (moderation status + verified flag at create/edit time),
 * {@link #explain} (the admin review breakdown), and the {@code lookup*} methods
 * (listing-form auto-fill).
 *
 * <p>Fails open (returns {@link PropertyStatus#ACTIVE}) whenever there's nothing
 * to check against: a legacy free-text address, or the mirror simply has no
 * record for that building/apartment/parcel yet.
 */
@Service
@RequiredArgsConstructor
public class CadastreQueryService {

    private static final double AREA_MISMATCH_RATIO = 0.15;
    private static final int YEAR_MISMATCH_TOLERANCE_YEARS = 5;

    private final CadastreRepository cadastreRepository;
    private final AddressRegistryRepository addressRegistryRepository;

    /** Per-aspect comparison outcome. NONE = one side missing, so nothing was compared. */
    private enum Aspect { MATCH, MISMATCH, NONE }

    @Transactional(readOnly = true)
    public CadastreDecision decideStatus(Long arBuildingCode, String apartment,
                                         String cadastreParcelNr, Listing listing) {
        Snapshot s = snapshot(arBuildingCode, apartment, cadastreParcelNr, listing);
        PropertyStatus status = s.anyMismatch() ? PropertyStatus.PENDING_REVIEW : PropertyStatus.ACTIVE;
        boolean verified = s.anyMatch() && !s.anyMismatch();
        return new CadastreDecision(status, verified);
    }

    @Transactional(readOnly = true)
    public CadastreComparison explain(Long arBuildingCode, String apartment,
                                      String cadastreParcelNr, Listing listing) {
        Snapshot s = snapshot(arBuildingCode, apartment, cadastreParcelNr, listing);
        return new CadastreComparison(
                listing.getYearBuilt(), s.officialYear, s.year == Aspect.MISMATCH,
                listing.getM2(), s.officialArea, s.area == Aspect.MISMATCH,
                listing.getLandM2(), s.officialLandM2, s.landArea == Aspect.MISMATCH,
                listing.getLandUse(), s.officialLandUse, s.landUse == Aspect.MISMATCH);
    }

    /** Official building/apartment figures for form auto-fill; nulls where unrecorded. */
    @Transactional(readOnly = true)
    public OfficialBuilding lookupBuilding(long arBuildingCode, String apartment) {
        Short year = cadastreRepository.findYearBuiltByArCode(arBuildingCode).orElse(null);
        BigDecimal area = officialApartmentArea(arBuildingCode, apartment);
        return new OfficialBuilding(year, area);
    }

    /** Official parcel figures for form auto-fill; nulls where unrecorded. */
    @Transactional(readOnly = true)
    public OfficialParcel lookupParcel(String cadastreParcelNr) {
        return cadastreRepository.findParcelByCadastreNr(cadastreParcelNr)
                .map(p -> new OfficialParcel(p.areaM2(), p.landUse()))
                .orElseGet(() -> new OfficialParcel(null, null));
    }

    // ── snapshot ──────────────────────────────────────────────

    /** All official figures + per-aspect comparison outcomes, looked up once. */
    private record Snapshot(
            Aspect year, Short officialYear,
            Aspect area, BigDecimal officialArea,
            Aspect landArea, BigDecimal officialLandM2,
            Aspect landUse, LandUse officialLandUse) {

        boolean anyMismatch() {
            return year == Aspect.MISMATCH || area == Aspect.MISMATCH
                    || landArea == Aspect.MISMATCH || landUse == Aspect.MISMATCH;
        }

        boolean anyMatch() {
            return year == Aspect.MATCH || area == Aspect.MATCH
                    || landArea == Aspect.MATCH || landUse == Aspect.MATCH;
        }
    }

    private Snapshot snapshot(Long arBuildingCode, String apartment,
                              String cadastreParcelNr, Listing listing) {
        Short officialYear = arBuildingCode == null ? null
                : cadastreRepository.findYearBuiltByArCode(arBuildingCode).orElse(null);
        BigDecimal officialArea = arBuildingCode == null ? null
                : officialApartmentArea(arBuildingCode, apartment);
        CadastreParcelRow parcel = cadastreParcelNr == null || cadastreParcelNr.isBlank() ? null
                : cadastreRepository.findParcelByCadastreNr(cadastreParcelNr).orElse(null);
        BigDecimal officialLandM2 = parcel == null ? null : parcel.areaM2();
        LandUse officialLandUse = parcel == null ? null : parcel.landUse();

        return new Snapshot(
                yearAspect(officialYear, listing.getYearBuilt()), officialYear,
                areaAspect(officialArea, listing.getM2()), officialArea,
                areaAspect(officialLandM2, listing.getLandM2()), officialLandM2,
                landUseAspect(officialLandUse, listing.getLandUse()), officialLandUse);
    }

    private BigDecimal officialApartmentArea(long arBuildingCode, String apartment) {
        if (apartment == null || apartment.isBlank()) {
            return null;
        }
        String normApartment = AddressMatcher.normalize(apartment);
        return addressRegistryRepository.findApartmentCode(arBuildingCode, normApartment)
                .flatMap(cadastreRepository::findAreaByArCode)
                .orElse(null);
    }

    // ── per-aspect comparison ─────────────────────────────────

    private static Aspect yearAspect(Short official, Short declared) {
        if (official == null || declared == null) {
            return Aspect.NONE;
        }
        return Math.abs(declared - official) > YEAR_MISMATCH_TOLERANCE_YEARS
                ? Aspect.MISMATCH : Aspect.MATCH;
    }

    private static Aspect areaAspect(BigDecimal official, BigDecimal declared) {
        if (official == null || declared == null) {
            return Aspect.NONE;
        }
        return relativeDiff(declared, official) > AREA_MISMATCH_RATIO
                ? Aspect.MISMATCH : Aspect.MATCH;
    }

    private static Aspect landUseAspect(LandUse official, LandUse declared) {
        if (official == null || declared == null) {
            return Aspect.NONE;
        }
        return official != declared ? Aspect.MISMATCH : Aspect.MATCH;
    }

    private static double relativeDiff(BigDecimal actual, BigDecimal official) {
        double officialValue = official.doubleValue();
        if (officialValue == 0) {
            return 0;
        }
        return Math.abs(actual.doubleValue() - officialValue) / officialValue;
    }
}
