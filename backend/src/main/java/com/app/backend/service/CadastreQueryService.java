package com.app.backend.service;

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
    public PropertyStatus decideStatus(Long arBuildingCode, String apartment, BigDecimal m2, Short yearBuilt) {
        if (arBuildingCode == null) {
            return PropertyStatus.ACTIVE;
        }
        boolean mismatch = yearMismatch(arBuildingCode, yearBuilt) || areaMismatch(arBuildingCode, apartment, m2);
        return mismatch ? PropertyStatus.PENDING_REVIEW : PropertyStatus.ACTIVE;
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
