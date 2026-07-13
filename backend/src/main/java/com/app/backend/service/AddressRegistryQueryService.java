package com.app.backend.service;

import com.app.backend.config.CacheConfig;
import com.app.backend.dto.address.BuildingAddress;
import com.app.backend.dto.address.BuildingOptionDto;
import com.app.backend.dto.address.StreetOptionDto;
import com.app.backend.dto.address.TerritoryRow;
import com.app.backend.repository.AddressRegistryRepository;
import com.app.backend.validation.AddressMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Street/house-number autocomplete over the State Address Register mirror.
 *
 * <p>The curated city/district slugs the frontend filters by don't map 1:1 to
 * register territories (Rīga's neighbourhoods aren't register objects; grouped
 * cities like "Ādaži & Carnikava" span several), so a search first resolves the
 * slugs to territory codes by normalized-name match: the district name wins
 * when it names a real territory compatible with the city (villages), otherwise
 * the city tokens themselves are looked up (cities, neighbourhood districts).
 */
@Service
@RequiredArgsConstructor
public class AddressRegistryQueryService {

    private static final int STREET_LIMIT = 20;
    private static final int BUILDING_LIMIT = 20;

    private final AddressRegistryRepository repository;

    @Cacheable(cacheNames = CacheConfig.ADDRESS_STREETS,
            key = "#citySlug + '|' + #districtSlug + '|' + #query")
    public List<StreetOptionDto> searchStreets(String citySlug, String districtSlug, String query) {
        List<Long> territories = resolveTerritories(citySlug, districtSlug);
        if (territories.isEmpty()) {
            return List.of();
        }
        String normQuery = AddressMatcher.normalize(query);
        List<StreetOptionDto> options =
                new ArrayList<>(repository.searchStreets(territories, normQuery, STREET_LIMIT));
        // Rural houses named directly under the territory fill the remaining slots
        // (in towns the street limit is usually reached and none show up).
        if (options.size() < STREET_LIMIT) {
            options.addAll(repository.searchTerritoryHouses(territories, normQuery,
                    STREET_LIMIT - options.size()));
        }
        return List.copyOf(options);
    }

    @Cacheable(cacheNames = CacheConfig.ADDRESS_BUILDINGS, key = "#streetCode + '|' + #query")
    public List<BuildingOptionDto> searchBuildings(long streetCode, String query) {
        return repository.searchBuildings(streetCode, AddressMatcher.normalize(query), BUILDING_LIMIT);
    }

    public Optional<BuildingAddress> findBuildingAddress(long code) {
        return repository.findBuildingAddress(code);
    }

    // ── territory resolution ──────────────────────────────────

    private List<Long> resolveTerritories(String citySlug, String districtSlug) {
        List<String> cityTokens = List.of(unslug(citySlug).split(" "));

        // 1. The district names a real territory (village/town in a grouped
        //    region) that belongs to one of the city's municipalities.
        List<TerritoryRow> byDistrict = repository.findTerritoriesByNormName(unslug(districtSlug));
        List<Long> compatible = byDistrict.stream()
                .filter(t -> matchesCity(t, cityTokens))
                .map(TerritoryRow::code)
                .toList();
        if (!compatible.isEmpty()) {
            return compatible;
        }

        // 2. The district is a neighbourhood (not a register object) — fall back
        //    to the city itself ("Rīga"), or its tokens for groups ("Ādaži & Carnikava").
        List<TerritoryRow> byCity = repository.findTerritoriesByNormName(unslug(citySlug));
        if (!byCity.isEmpty()) {
            return byCity.stream().map(TerritoryRow::code).toList();
        }
        Set<Long> union = new LinkedHashSet<>();
        for (String token : cityTokens) {
            for (TerritoryRow t : repository.findTerritoriesByNormName(token)) {
                union.add(t.code());
            }
        }
        return List.copyOf(union);
    }

    /**
     * Whether a district-matched territory plausibly belongs to the selected
     * city: its municipality (or, for republic cities, its own name) must share
     * a declension stem with one of the city-name tokens ("Ādaži" ~ "Ādažu nov.").
     * Rejects same-named villages elsewhere in Latvia.
     */
    private static boolean matchesCity(TerritoryRow territory, List<String> cityTokens) {
        String reference = territory.normNovadsName() != null
                ? territory.normNovadsName()
                : territory.normName();
        String firstWord = reference.split(" ")[0];
        return cityTokens.stream().anyMatch(token -> stemEquals(token, firstWord));
    }

    /** Equality up to the last letter of the shorter word (Latvian nominative vs genitive: adazi/adazu). */
    private static boolean stemEquals(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        int min = Math.min(a.length(), b.length());
        if (min < 4) {
            return false;
        }
        int common = 0;
        while (common < min && a.charAt(common) == b.charAt(common)) {
            common++;
        }
        return common >= min - 1;
    }

    /** Slug → the normalized form used in the mirror's norm_name columns. */
    private static String unslug(String slug) {
        return AddressMatcher.normalize(slug.replace('-', ' '));
    }
}
