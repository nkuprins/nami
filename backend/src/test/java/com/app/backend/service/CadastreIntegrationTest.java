package com.app.backend.service;

import com.app.backend.IntegrationTestBase;
import com.app.backend.dto.cadastre.CadastreIngestStats;
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
    }

    @Autowired private AddressRegistryIngestService addressIngestService;
    @Autowired private CadastreIngestService cadastreIngestService;
    @Autowired private CadastreQueryService cadastreQueryService;

    @Test
    void ingest_thenMismatchCheckResolvesBuildingAndApartment() throws Exception {
        addressIngestService.ingest();
        CadastreIngestStats stats = cadastreIngestService.ingest();

        // 2 buildings in the fixture (one with a VARISCode, one without); 2 premises.
        assertThat(stats).isEqualTo(new CadastreIngestStats(2, 2));

        // No cadastre link at all (legacy free-text address) — fails open.
        assertThat(cadastreQueryService.decideStatus(null, null, new BigDecimal("999"), (short) 1900))
                .isEqualTo(PropertyStatus.ACTIVE);

        // Building 6001: cadastre year is 1985. Within tolerance (5 years) → ACTIVE.
        assertThat(cadastreQueryService.decideStatus(6001L, null, new BigDecimal("52.8"), (short) 1983))
                .isEqualTo(PropertyStatus.ACTIVE);
        // Outside tolerance → PENDING_REVIEW.
        assertThat(cadastreQueryService.decideStatus(6001L, null, new BigDecimal("52.8"), (short) 1950))
                .isEqualTo(PropertyStatus.PENDING_REVIEW);

        // Apartment "1" under building 6001 (VAR code 7001) has cadastre area 52.8 m².
        assertThat(cadastreQueryService.decideStatus(6001L, "1", new BigDecimal("53.0"), null))
                .isEqualTo(PropertyStatus.ACTIVE); // within 15%
        assertThat(cadastreQueryService.decideStatus(6001L, "1", new BigDecimal("90.0"), null))
                .isEqualTo(PropertyStatus.PENDING_REVIEW); // way over 15%

        // Building 6002 has no VARISCode in the fixture, so it never resolves — fails open.
        assertThat(cadastreQueryService.decideStatus(6002L, null, new BigDecimal("999"), (short) 1900))
                .isEqualTo(PropertyStatus.ACTIVE);

        // Unknown apartment under a known building — fails open on the area check alone.
        assertThat(cadastreQueryService.decideStatus(6001L, "99", new BigDecimal("999"), null))
                .isEqualTo(PropertyStatus.ACTIVE);
    }
}
