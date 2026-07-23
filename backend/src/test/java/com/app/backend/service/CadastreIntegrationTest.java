package com.app.backend.service;

import com.app.backend.IntegrationTestBase;
import com.app.backend.dto.cadastre.CadastreComparison;
import com.app.backend.dto.cadastre.CadastreDecision;
import com.app.backend.dto.cadastre.CadastreIngestStats;
import com.app.backend.entity.Listing;
import com.app.backend.enums.LandUse;
import com.app.backend.enums.PropertyStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ingests the sample cadastre zips (file: URLs) and the sample address-register
 * CSVs, then exercises the mismatch check end to end: building 6001 in the
 * address fixtures resolves to a cadastre year of 1985, and its apartment "1"
 * (VAR code 7001) resolves to a cadastre area of 52.8 m².
 */
class CadastreIntegrationTest extends IntegrationTestBase {

    @DynamicPropertySource
    static void registerUrls(DynamicPropertyRegistry registry) {
        Path addressDir = Path.of("src/test/resources/addressregister").toAbsolutePath();
        registry.add("app.address-register.urls.novads", () -> addressDir.resolve("aw_novads.csv").toUri());
        registry.add("app.address-register.urls.pagasts", () -> addressDir.resolve("aw_pagasts.csv").toUri());
        registry.add("app.address-register.urls.pilseta", () -> addressDir.resolve("aw_pilseta.csv").toUri());
        registry.add("app.address-register.urls.ciems", () -> addressDir.resolve("aw_ciems.csv").toUri());
        registry.add("app.address-register.urls.iela", () -> addressDir.resolve("aw_iela.csv").toUri());
        registry.add("app.address-register.urls.eka", () -> addressDir.resolve("aw_eka.csv").toUri());
        registry.add("app.address-register.urls.dziv", () -> addressDir.resolve("aw_dziv.csv").toUri());

        Path cadastreDir = Path.of("src/test/resources/cadastre").toAbsolutePath();
        registry.add("app.cadastre.urls.building", () -> cadastreDir.resolve("building.zip").toUri());
        registry.add("app.cadastre.urls.premise-group", () -> cadastreDir.resolve("premisegroup.zip").toUri());
        registry.add("app.cadastre.urls.parcel", () -> cadastreDir.resolve("parcel.zip").toUri());
    }

    private static Listing building(BigDecimal m2, Short yearBuilt) {
        Listing l = new Listing();
        l.setM2(m2);
        l.setYearBuilt(yearBuilt);
        return l;
    }

    private static Listing parcel(BigDecimal landM2, LandUse landUse) {
        Listing l = new Listing();
        l.setLandM2(landM2);
        l.setLandUse(landUse);
        return l;
    }

    @Autowired private AddressRegistryIngestService addressIngestService;
    @Autowired private CadastreIngestService cadastreIngestService;
    @Autowired private CadastreQueryService cadastreQueryService;

    @Test
    void ingest_thenMismatchCheckResolvesBuildingAndApartment() throws Exception {
        addressIngestService.ingest();
        CadastreIngestStats stats = cadastreIngestService.ingest();

        // 2 buildings in the fixture (one with a VARISCode, one without); 2 premises; 2 parcels.
        assertThat(stats).isEqualTo(new CadastreIngestStats(2, 2, 2));

        // No cadastre link at all (legacy free-text address) — fails open, and with
        // nothing compared it is ACTIVE but not verified.
        CadastreDecision noLink = cadastreQueryService.decideStatus(
                null, null, null, building(new BigDecimal("999"), (short) 1900));
        assertThat(noLink.status()).isEqualTo(PropertyStatus.ACTIVE);
        assertThat(noLink.verified()).isFalse();

        // Building 6001: cadastre year is 1985. Within tolerance (5 years) → ACTIVE, and
        // since a figure was compared and matched, verified.
        CadastreDecision yearMatch = cadastreQueryService.decideStatus(
                6001L, null, null, building(new BigDecimal("52.8"), (short) 1983));
        assertThat(yearMatch.status()).isEqualTo(PropertyStatus.ACTIVE);
        assertThat(yearMatch.verified()).isTrue();

        // Outside tolerance → PENDING_REVIEW, not verified.
        CadastreDecision yearMiss = cadastreQueryService.decideStatus(
                6001L, null, null, building(new BigDecimal("52.8"), (short) 1950));
        assertThat(yearMiss.status()).isEqualTo(PropertyStatus.PENDING_REVIEW);
        assertThat(yearMiss.verified()).isFalse();

        // Apartment "1" under building 6001 (VAR code 7001) has cadastre area 52.8 m².
        assertThat(cadastreQueryService.decideStatus(6001L, "1", null, building(new BigDecimal("53.0"), null))
                .status()).isEqualTo(PropertyStatus.ACTIVE); // within 15%
        assertThat(cadastreQueryService.decideStatus(6001L, "1", null, building(new BigDecimal("90.0"), null))
                .status()).isEqualTo(PropertyStatus.PENDING_REVIEW); // way over 15%

        // Building 6002 has no VARISCode in the fixture, so it never resolves — fails open.
        assertThat(cadastreQueryService.decideStatus(6002L, null, null, building(new BigDecimal("999"), (short) 1900))
                .status()).isEqualTo(PropertyStatus.ACTIVE);

        // Unknown apartment under a known building — fails open on the area check alone.
        assertThat(cadastreQueryService.decideStatus(6001L, "99", null, building(new BigDecimal("999"), null))
                .status()).isEqualTo(PropertyStatus.ACTIVE);

        // ── land parcels ──────────────────────────────────────────
        // Parcel 21000030512: area 1200 m², dominant land-purpose 0801 → COMMERCIAL
        // (multi-use in the fixture; the larger-area purpose wins).
        // Declared plot area and land-use both match → ACTIVE and verified.
        CadastreDecision parcelMatch = cadastreQueryService.decideStatus(
                null, null, "21000030512", parcel(new BigDecimal("1200.00"), LandUse.COMMERCIAL));
        assertThat(parcelMatch.status()).isEqualTo(PropertyStatus.ACTIVE);
        assertThat(parcelMatch.verified()).isTrue();
        // Plot area way over the 15% tolerance → PENDING_REVIEW.
        assertThat(cadastreQueryService.decideStatus(null, null, "21000030512",
                parcel(new BigDecimal("3000.00"), LandUse.COMMERCIAL)).status())
                .isEqualTo(PropertyStatus.PENDING_REVIEW);
        // Declared land-use disagrees with the cadastre → PENDING_REVIEW.
        assertThat(cadastreQueryService.decideStatus(null, null, "21000030512",
                parcel(new BigDecimal("1200.00"), LandUse.RESIDENTIAL)).status())
                .isEqualTo(PropertyStatus.PENDING_REVIEW);
        // Unknown parcel number — fails open.
        assertThat(cadastreQueryService.decideStatus(null, null, "99990000000",
                parcel(new BigDecimal("1200.00"), LandUse.COMMERCIAL)).status())
                .isEqualTo(PropertyStatus.ACTIVE);
        // No parcel link at all — fails open.
        assertThat(cadastreQueryService.decideStatus(null, null, null,
                parcel(new BigDecimal("1200.00"), LandUse.COMMERCIAL)).status())
                .isEqualTo(PropertyStatus.ACTIVE);

        // ── explain() drives the admin review breakdown ───────────
        CadastreComparison cmp = cadastreQueryService.explain(
                6001L, null, null, building(new BigDecimal("52.8"), (short) 1950));
        assertThat(cmp.declaredYear()).isEqualTo((short) 1950);
        assertThat(cmp.officialYear()).isEqualTo((short) 1985);
        assertThat(cmp.yearMismatch()).isTrue();

        // ── lookup() drives listing-form auto-fill ────────────────
        assertThat(cadastreQueryService.lookupBuilding(6001L, "1").area())
                .isEqualByComparingTo(new BigDecimal("52.8"));
        assertThat(cadastreQueryService.lookupParcel("21000030512").landUse())
                .isEqualTo(LandUse.COMMERCIAL);
    }
}
