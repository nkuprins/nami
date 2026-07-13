package com.app.backend.service;

import com.app.backend.IntegrationTestBase;
import com.app.backend.dto.address.AddressIngestStats;
import com.app.backend.dto.address.BuildingOptionDto;
import com.app.backend.dto.address.StreetOptionDto;
import com.app.backend.repository.AddressRegistryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ingests the sample register CSVs (file: URLs) and exercises the autocomplete
 * stack end to end: territory resolution from curated slugs, street and
 * house-number search, rural named houses, and the public endpoints.
 */
class AddressRegistryIntegrationTest extends IntegrationTestBase {

    @DynamicPropertySource
    static void registerCsvUrls(DynamicPropertyRegistry registry) {
        Path dir = Path.of("src/test/resources/addressregister").toAbsolutePath();
        registry.add("app.address-register.urls.novads", () -> dir.resolve("aw_novads.csv").toUri());
        registry.add("app.address-register.urls.pagasts", () -> dir.resolve("aw_pagasts.csv").toUri());
        registry.add("app.address-register.urls.pilseta", () -> dir.resolve("aw_pilseta.csv").toUri());
        registry.add("app.address-register.urls.ciems", () -> dir.resolve("aw_ciems.csv").toUri());
        registry.add("app.address-register.urls.iela", () -> dir.resolve("aw_iela.csv").toUri());
        registry.add("app.address-register.urls.eka", () -> dir.resolve("aw_eka.csv").toUri());
    }

    @Autowired private AddressRegistryIngestService ingestService;
    @Autowired private AddressRegistryQueryService queryService;
    @Autowired private AddressRegistryRepository registryRepository;

    @Test
    void ingest_thenAutocompleteResolvesTerritoriesStreetsAndBuildings() throws Exception {
        AddressIngestStats stats = ingestService.ingest();

        // DEL rows and orphans are dropped: 1 city + 2 parishes + 3 villages; 4 streets; 6 buildings.
        assertThat(stats).isEqualTo(new AddressIngestStats(6, 4, 6));

        // A Rīga neighbourhood isn't a register territory — falls back to the city's streets.
        List<StreetOptionDto> riga = queryService.searchStreets("riga", "teika", "briv");
        assertThat(riga).extracting(StreetOptionDto::name).containsExactly("Brīvības iela");
        assertThat(riga.getFirst().kind()).isEqualTo("street");

        // A grouped city ("Ādaži & Carnikava") with a village district searches only that village.
        List<StreetOptionDto> kalngale = queryService.searchStreets("adazi-&-carnikava", "kalngale", "");
        assertThat(kalngale).extracting(StreetOptionDto::name)
                .containsExactly("Skolas iela", "Vecvagari"); // street first, then the rural named house
        assertThat(kalngale.getLast().kind()).isEqualTo("house");
        assertThat(kalngale.getLast().lat()).isEqualTo(57.083);

        // Same-named villages elsewhere are rejected by municipality: Mārupe's
        // Skulte must not surface streets from Skulte in Limbažu nov.
        List<StreetOptionDto> skulte = queryService.searchStreets("marupe", "skulte", "");
        assertThat(skulte).extracting(StreetOptionDto::name).containsExactly("Jūras iela");

        // House numbers under a street: prefix-filtered, numerically friendly order.
        long brivibasCode = riga.getFirst().code();
        List<BuildingOptionDto> houses = queryService.searchBuildings(brivibasCode, "1");
        assertThat(houses).extracting(BuildingOptionDto::name).containsExactly("12", "14", "12 k-1");
        assertThat(houses.getFirst().lat()).isEqualTo(56.951);

        // Unknown locations resolve to nothing rather than leaking other cities.
        assertThat(queryService.searchStreets("atlantida", "centrs", "iela")).isEmpty();

        // Building lookup used by property creation resolves street and house names.
        assertThat(registryRepository.findBuildingAddress(6001L))
                .hasValueSatisfying(b -> {
                    assertThat(b.streetName()).isEqualTo("Brīvības iela");
                    assertThat(b.houseName()).isEqualTo("12");
                });
        assertThat(registryRepository.findBuildingAddress(6006L))
                .hasValueSatisfying(b -> {
                    assertThat(b.streetName()).isNull();
                    assertThat(b.houseName()).isEqualTo("Riņņi");
                });

        // Public endpoints expose the same data without authentication.
        mockMvc.perform(get("/api/address/streets")
                        .param("city", "riga").param("district", "teika").param("q", "briv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Brīvības iela"))
                .andExpect(jsonPath("$[0].kind").value("street"));
        mockMvc.perform(get("/api/address/buildings")
                        .param("streetCode", String.valueOf(brivibasCode)).param("q", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("12"))
                .andExpect(jsonPath("$[1].name").value("12 k-1"));

        // Re-ingest is idempotent (wipe and reload).
        assertThat(ingestService.ingest()).isEqualTo(stats);
    }
}
