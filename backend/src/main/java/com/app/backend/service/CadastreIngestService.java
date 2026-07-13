package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.cadastre.CadastreBuildingRow;
import com.app.backend.dto.cadastre.CadastreIngestStats;
import com.app.backend.dto.cadastre.CadastrePremiseRow;
import com.app.backend.repository.CadastreRepository;
import com.app.backend.util.XmlStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Mirrors VZD's Cadastre open data (Building and PremiseGroup exports,
 * CC-BY-4.0) into the {@code cadastre_*} tables backing the listing-review
 * mismatch check. Each source is a zip of many territory-split XML files
 * (some hundreds of MB uncompressed), streamed one entry at a time so the
 * full export never has to fit in memory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CadastreIngestService {

    private static final int BATCH_SIZE = 1_000;
    private static final String BUILDING_ITEM = "BuildingItemData";
    private static final String PREMISE_ITEM = "PremiseGroupItemData";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final CadastreRepository repository;
    private final AppProperties appProperties;
    private final TransactionTemplate transactionTemplate;

    public CadastreIngestStats ingest() throws IOException, InterruptedException {
        AppProperties.CadastreUrls urls = appProperties.cadastre().urls();
        List<Path> temp = new ArrayList<>();
        try {
            Path buildingZip = download(urls.building(), temp);
            Path premiseGroupZip = download(urls.premiseGroup(), temp);

            CadastreIngestStats stats = Objects.requireNonNull(transactionTemplate.execute(tx -> {
                repository.deleteAll();
                int buildings = loadBuildings(buildingZip);
                int premises = loadPremises(premiseGroupZip);
                return new CadastreIngestStats(buildings, premises);
            }));

            log.info("Cadastre ingest done: {} buildings, {} premises", stats.buildings(), stats.premises());
            return stats;
        } finally {
            for (Path path : temp) {
                Files.deleteIfExists(path);
            }
        }
    }

    // ── download ──────────────────────────────────────────────

    private static Path download(String url, List<Path> temp) throws IOException, InterruptedException {
        Path target = Files.createTempFile("vzd-cadastre-", ".zip");
        temp.add(target);
        URI uri = URI.create(url);
        if ("file".equals(uri.getScheme())) {
            Files.copy(Path.of(uri), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return target;
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(30))
                .GET()
                .build();
        HttpResponse<Path> response = HTTP.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() != 200) {
            throw new IOException("Download of " + url + " failed with HTTP " + response.statusCode());
        }
        return target;
    }

    // ── zip of XML → rows ─────────────────────────────────────

    private int loadBuildings(Path zipPath) {
        List<CadastreBuildingRow> batch = new ArrayList<>(BATCH_SIZE);
        int[] total = {0};
        forEachItem(zipPath, BUILDING_ITEM, item -> {
            String cadastreNr = item.get("BuildingCadastreNr");
            if (cadastreNr == null || cadastreNr.isBlank()) {
                return;
            }
            batch.add(new CadastreBuildingRow(cadastreNr,
                    parseLong(item.get("VARISCode")), parseYear(item.get("BuildingExploitYear"))));
            if (batch.size() == BATCH_SIZE) {
                repository.insertBuildings(List.copyOf(batch));
                total[0] += batch.size();
                batch.clear();
            }
        });
        if (!batch.isEmpty()) {
            repository.insertBuildings(List.copyOf(batch));
            total[0] += batch.size();
        }
        return total[0];
    }

    private int loadPremises(Path zipPath) {
        List<CadastrePremiseRow> batch = new ArrayList<>(BATCH_SIZE);
        int[] total = {0};
        forEachItem(zipPath, PREMISE_ITEM, item -> {
            String cadastreNr = item.get("PremiseGroupCadastreNr");
            if (cadastreNr == null || cadastreNr.isBlank()) {
                return;
            }
            batch.add(new CadastrePremiseRow(cadastreNr,
                    parseLong(item.get("PremiseGroupVARISCode")), parseArea(item.get("PremiseGroupArea"))));
            if (batch.size() == BATCH_SIZE) {
                repository.insertPremises(List.copyOf(batch));
                total[0] += batch.size();
                batch.clear();
            }
        });
        if (!batch.isEmpty()) {
            repository.insertPremises(List.copyOf(batch));
            total[0] += batch.size();
        }
        return total[0];
    }

    /** Streams every {@code *.xml} entry in the zip, feeding each {@code itemLocalName} element to the consumer. */
    private static void forEachItem(Path zipPath, String itemLocalName, java.util.function.Consumer<Map<String, String>> consumer) {
        try (var in = Files.newInputStream(zipPath); ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".xml")) {
                    continue;
                }
                // The StAX reader's close() must not reach the shared ZipInputStream —
                // more entries follow it — so it only ever sees a non-closing wrapper.
                try (XmlStream xml = XmlStream.open(nonClosing(zis), itemLocalName)) {
                    Map<String, String> item;
                    while ((item = xml.next()) != null) {
                        consumer.accept(item);
                    }
                } catch (XMLStreamException e) {
                    throw new UncheckedIOException(new IOException("Malformed cadastre XML in " + entry.getName(), e));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static java.io.InputStream nonClosing(java.io.InputStream in) {
        return new java.io.FilterInputStream(in) {
            @Override
            public void close() {
                // no-op: the caller closes the real ZipInputStream once, after all entries
            }
        };
    }

    // ── helpers ───────────────────────────────────────────────

    private static Long parseLong(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value.trim());
    }

    /** BuildingExploitYear is an xs:gYear ("1985", occasionally "1985Z"/"1985+02:00"). */
    private static Short parseYear(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.trim().substring(0, 4);
        return Short.valueOf(digits);
    }

    private static BigDecimal parseArea(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
    }
}
