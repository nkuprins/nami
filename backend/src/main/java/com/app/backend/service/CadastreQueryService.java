package com.app.backend.service;

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
 * Cross-checks a listing's posted area and build year against the VZD
 * cadastre mirror, deciding whether it can go live immediately or should be
 * held for admin review. Fails open (returns {@link PropertyStatus#ACTIVE})
 * whenever there's nothing to check against: a legacy free-text address, or
 * the cadastre mirror simply has no record for that building/apartment yet.
 */
@Service
@RequiredArgsConstructor
public class CadastreQueryService {

    private static final double AREA_MISMATCH_RATIO = 0.15;
    private static final int YEAR_MISMATCH_TOLERANCE_YEARS = 5;

    private final CadastreRepository cadastreRepository;
    private final AddressRegistryRepository addressRegistryRepository;

    @Transactional(readOnly = true)
    public PropertyStatus decideStatus(Long arBuildingCode, String apartment,
                                       String cadastreParcelNr, Listing listing) {
        boolean buildingMismatch = arBuildingCode != null
                && (yearMismatch(arBuildingCode, listing.getYearBuilt())
                || areaMismatch(arBuildingCode, apartment, listing.getM2()));
        boolean mismatch = buildingMismatch || parcelMismatch(cadastreParcelNr, listing);
        return mismatch ? PropertyStatus.PENDING_REVIEW : PropertyStatus.ACTIVE;
    }

    /**
     * Cross-checks a land/commercial listing's declared plot area and land-use
     * purpose against the cadastre parcel it was linked to. Fails open when the
     * property has no parcel link or the mirror has no record for it.
     */
    private boolean parcelMismatch(String cadastreParcelNr, Listing listing) {
        if (cadastreParcelNr == null || cadastreParcelNr.isBlank()) {
            return false;
        }
        return cadastreRepository.findParcelByCadastreNr(cadastreParcelNr)
                .map(parcel -> parcelAreaMismatch(listing.getLandM2(), parcel.areaM2())
                        || landUseMismatch(listing.getLandUse(), parcel.landUse()))
                .orElse(false);
    }

    private boolean parcelAreaMismatch(BigDecimal declared, BigDecimal official) {
        if (declared == null || official == null) {
            return false;
        }
        return relativeDiff(declared, official) > AREA_MISMATCH_RATIO;
    }

    private boolean landUseMismatch(LandUse declared, LandUse official) {
        return declared != null && official != null && declared != official;
    }

    private boolean yearMismatch(long arBuildingCode, Short yearBuilt) {
        if (yearBuilt == null) {
            return false;
        }
        return cadastreRepository.findYearBuiltByArCode(arBuildingCode)
                .map(officialYear -> Math.abs(yearBuilt - officialYear) > YEAR_MISMATCH_TOLERANCE_YEARS)
                .orElse(false);
    }

    private boolean areaMismatch(long arBuildingCode, String apartment, BigDecimal m2) {
        if (m2 == null || apartment == null || apartment.isBlank()) {
            return false;
        }
        String normApartment = AddressMatcher.normalize(apartment);
        return addressRegistryRepository.findApartmentCode(arBuildingCode, normApartment)
                .flatMap(cadastreRepository::findAreaByArCode)
                .map(officialArea -> relativeDiff(m2, officialArea) > AREA_MISMATCH_RATIO)
                .orElse(false);
    }

    private static double relativeDiff(BigDecimal actual, BigDecimal official) {
        double officialValue = official.doubleValue();
        if (officialValue == 0) {
            return 0;
        }
        return Math.abs(actual.doubleValue() - officialValue) / officialValue;
    }
}
