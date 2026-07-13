package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.config.CacheConfig;
import com.app.backend.dto.address.AddressIngestStats;
import com.app.backend.dto.address.BuildingRow;
import com.app.backend.dto.address.StreetRow;
import com.app.backend.dto.address.TerritoryRow;
import com.app.backend.repository.AddressRegistryRepository;
import com.app.backend.util.CsvStream;
import com.app.backend.validation.AddressMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Mirrors Latvia's State Address Register (VZD open data, CC-BY-4.0) into the
 * {@code address_*} tables powering strict street/house autocomplete.
 *
 * <p>Each run downloads the register CSVs, then wipes and reloads the mirror in
 * one transaction: territories (cities 104, parishes 105, villages 106) with
 * their municipality resolved through the parent chain, streets, and buildings.
 * Only rows with status {@code EKS} (existing) are kept. Buildings are streamed
 * in batches because the file holds ~1M rows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressRegistryIngestService {

    private static final int BATCH_SIZE = 1_000;
    private static final String STATUS_EXISTING = "EKS";
    private static final int TYPE_CITY = 104;
    private static final int TYPE_PARISH = 105;
    private static final int TYPE_VILLAGE = 106;
    private static final int TYPE_STREET = 107;
    private static final int TYPE_NOVADS = 113;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final AddressRegistryRepository repository;
    private final AppProperties appProperties;
    private final TransactionTemplate transactionTemplate;
    private final CacheManager cacheManager;

    public AddressIngestStats ingest() throws IOException, InterruptedException {
        AppProperties.AddressRegisterUrls urls = appProperties.addressRegister().urls();
        List<Path> temp = new ArrayList<>();
        try {
            Path novadi = download(urls.novads(), temp);
            Path pagasti = download(urls.pagasts(), temp);
            Path pilsetas = download(urls.pilseta(), temp);
            Path ciemi = download(urls.ciems(), temp);
            Path ielas = download(urls.iela(), temp);
            Path ekas = download(urls.eka(), temp);

            Map<Long, String> novadsNames = readNovadsNames(novadi);
            List<TerritoryRow> territories = new ArrayList<>();
            Map<Long, String> parishNovads = readParishes(pagasti, novadsNames, territories);
            readCities(pilsetas, novadsNames, territories);
            readVillages(ciemi, novadsNames, parishNovads, territories);

            Set<Long> territoryCodes = new HashSet<>();
            for (TerritoryRow t : territories) {
                territoryCodes.add(t.code());
            }
            Map<Long, Long> streetTerritory = new HashMap<>();
            List<StreetRow> streets = readStreets(ielas, territoryCodes, streetTerritory);

            AddressIngestStats stats = Objects.requireNonNull(transactionTemplate.execute(tx -> {
                repository.deleteAll();
                for (List<TerritoryRow> batch : batches(territories)) {
                    repository.insertTerritories(batch);
                }
                for (List<StreetRow> batch : batches(streets)) {
                    repository.insertStreets(batch);
                }
                int buildings = loadBuildings(ekas, territoryCodes, streetTerritory);
                return new AddressIngestStats(territories.size(), streets.size(), buildings);
            }));

            evictAddressCaches();
            log.info("Address register ingest done: {} territories, {} streets, {} buildings",
                    stats.territories(), stats.streets(), stats.buildings());
            return stats;
        } finally {
            for (Path path : temp) {
                Files.deleteIfExists(path);
            }
        }
    }

    // ── download ──────────────────────────────────────────────

    private static Path download(String url, List<Path> temp) throws IOException, InterruptedException {
        Path target = Files.createTempFile("vzd-", ".csv");
        temp.add(target);
        URI uri = URI.create(url);
        if ("file".equals(uri.getScheme())) {
            Files.copy(Path.of(uri), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return target;
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(10))
                .GET()
                .build();
        HttpResponse<Path> response = HTTP.send(request,
                HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() != 200) {
            throw new IOException("Download of " + url + " failed with HTTP " + response.statusCode());
        }
        return target;
    }

    // ── CSV → rows ────────────────────────────────────────────

    private static Map<Long, String> readNovadsNames(Path file) throws IOException {
        Map<Long, String> names = new HashMap<>();
        forEachExisting(file, (csv, row) ->
                names.put(parseLong(csv.col(row, "KODS")), csv.col(row, "NOSAUKUMS")));
        return names;
    }

    /** Parishes are territories themselves (rural houses hang under them). Returns parish code → novads name. */
    private static Map<Long, String> readParishes(Path file, Map<Long, String> novadsNames,
                                                  List<TerritoryRow> territories) throws IOException {
        Map<Long, String> parishNovads = new HashMap<>();
        forEachExisting(file, (csv, row) -> {
            long code = parseLong(csv.col(row, "KODS"));
            String novads = novadsNames.get(parseLong(csv.col(row, "VKUR_CD")));
            parishNovads.put(code, novads);
            territories.add(territory(code, TYPE_PARISH, csv.col(row, "NOSAUKUMS"), novads));
        });
        return parishNovads;
    }

    private static void readCities(Path file, Map<Long, String> novadsNames,
                                   List<TerritoryRow> territories) throws IOException {
        forEachExisting(file, (csv, row) -> {
            // Republic cities hang under the country (101) and keep a null novads.
            String novads = parseInt(csv.col(row, "VKUR_TIPS")) == TYPE_NOVADS
                    ? novadsNames.get(parseLong(csv.col(row, "VKUR_CD")))
                    : null;
            territories.add(territory(parseLong(csv.col(row, "KODS")), TYPE_CITY,
                    csv.col(row, "NOSAUKUMS"), novads));
        });
    }

    private static void readVillages(Path file, Map<Long, String> novadsNames,
                                     Map<Long, String> parishNovads,
                                     List<TerritoryRow> territories) throws IOException {
        forEachExisting(file, (csv, row) -> {
            long parent = parseLong(csv.col(row, "VKUR_CD"));
            String novads = parseInt(csv.col(row, "VKUR_TIPS")) == TYPE_PARISH
                    ? parishNovads.get(parent)
                    : novadsNames.get(parent);
            territories.add(territory(parseLong(csv.col(row, "KODS")), TYPE_VILLAGE,
                    csv.col(row, "NOSAUKUMS"), novads));
        });
    }

    private static List<StreetRow> readStreets(Path file, Set<Long> territoryCodes,
                                               Map<Long, Long> streetTerritory) throws IOException {
        List<StreetRow> streets = new ArrayList<>();
        forEachExisting(file, (csv, row) -> {
            long territoryCode = parseLong(csv.col(row, "VKUR_CD"));
            if (!territoryCodes.contains(territoryCode)) {
                return; // parent territory is deleted/foreign — orphan street
            }
            long code = parseLong(csv.col(row, "KODS"));
            String name = csv.col(row, "NOSAUKUMS");
            streets.add(new StreetRow(code, territoryCode, name, AddressMatcher.normalize(name)));
            streetTerritory.put(code, territoryCode);
        });
        return streets;
    }

    /** Streams the ~1M-row building file straight into batched inserts (runs inside the reload transaction). */
    private int loadBuildings(Path file, Set<Long> territoryCodes,
                              Map<Long, Long> streetTerritory) {
        List<BuildingRow> batch = new ArrayList<>(BATCH_SIZE);
        int[] total = {0};
        try {
            forEachExisting(file, (csv, row) -> {
                long parent = parseLong(csv.col(row, "VKUR_CD"));
                Long streetCode;
                long territoryCode;
                if (parseInt(csv.col(row, "VKUR_TIPS")) == TYPE_STREET) {
                    Long territory = streetTerritory.get(parent);
                    if (territory == null) {
                        return; // parent street was skipped
                    }
                    streetCode = parent;
                    territoryCode = territory;
                } else {
                    if (!territoryCodes.contains(parent)) {
                        return;
                    }
                    streetCode = null;
                    territoryCode = parent;
                }
                String name = csv.col(row, "NOSAUKUMS");
                if (name.isBlank()) {
                    return;
                }
                batch.add(new BuildingRow(parseLong(csv.col(row, "KODS")), streetCode, territoryCode,
                        name, AddressMatcher.normalize(name),
                        parseDouble(csv.col(row, "DD_N")), parseDouble(csv.col(row, "DD_E"))));
                if (batch.size() == BATCH_SIZE) {
                    repository.insertBuildings(List.copyOf(batch));
                    total[0] += batch.size();
                    batch.clear();
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!batch.isEmpty()) {
            repository.insertBuildings(List.copyOf(batch));
            total[0] += batch.size();
        }
        return total[0];
    }

    // ── helpers ───────────────────────────────────────────────

    /** Iterates a register CSV, keeping only well-formed rows with status EKS (existing). */
    private static void forEachExisting(Path file, BiConsumer<CsvStream, String[]> consumer) throws IOException {
        try (InputStream in = Files.newInputStream(file); CsvStream csv = CsvStream.open(in)) {
            String[] row;
            while ((row = csv.next()) != null) {
                if (row.length < 2) {
                    continue; // stray blank line
                }
                if (STATUS_EXISTING.equals(csv.col(row, "STATUSS"))) {
                    consumer.accept(csv, row);
                }
            }
        }
    }

    private static TerritoryRow territory(long code, int typeCd, String name, String novadsName) {
        return new TerritoryRow(code, typeCd, name, AddressMatcher.normalize(name),
                novadsName, novadsName != null ? AddressMatcher.normalize(novadsName) : null);
    }

    private static <T> List<List<T>> batches(List<T> rows) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i += BATCH_SIZE) {
            result.add(rows.subList(i, Math.min(i + BATCH_SIZE, rows.size())));
        }
        return result;
    }

    private static long parseLong(String value) {
        return Long.parseLong(value.trim());
    }

    private static int parseInt(String value) {
        return value.isBlank() ? -1 : Integer.parseInt(value.trim());
    }

    private static Double parseDouble(String value) {
        return value.isBlank() ? null : Double.valueOf(value.trim());
    }

    private void evictAddressCaches() {
        for (String cacheName : List.of(CacheConfig.ADDRESS_STREETS, CacheConfig.ADDRESS_BUILDINGS)) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
