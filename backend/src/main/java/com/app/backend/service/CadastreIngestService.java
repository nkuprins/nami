package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.cadastre.CadastreBuildingRow;
import com.app.backend.dto.cadastre.CadastreIngestStats;
import com.app.backend.dto.cadastre.CadastreParcelRow;
import com.app.backend.dto.cadastre.CadastrePremiseRow;
import com.app.backend.enums.LandUse;
import com.app.backend.repository.CadastreRepository;
import com.app.backend.util.XmlStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
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
    // Parcel (zemes vienība) export. Unlike Building/PremiseGroup, a parcel's fields
    // (ParcelCadastreNr, ParcelArea) sit under ParcelBasicData and its land-use lives in
    // a *repeating* LandPurposeList/LandPurposeData group — so parcels get a dedicated
    // parse (loadParcels/readParcel) rather than the flat XmlStream Map view.
    private static final String PARCEL_ITEM = "ParcelItemData";
    private static final String LAND_PURPOSE = "LandPurposeData";

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
            Path parcelZip = download(urls.parcel(), temp);

            CadastreIngestStats stats = Objects.requireNonNull(transactionTemplate.execute(tx -> {
                repository.deleteAll();
                int buildings = loadBuildings(buildingZip);
                int premises = loadPremises(premiseGroupZip);
                int parcels = loadParcels(parcelZip);
                return new CadastreIngestStats(buildings, premises, parcels);
            }));

            log.info("Cadastre ingest done: {} buildings, {} premises, {} parcels",
                    stats.buildings(), stats.premises(), stats.parcels());
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

    /**
     * Streams the parcel zip. A parcel carries repeating {@code LandPurposeData} entries
     * (mixed-use plots), so this reads each {@code ParcelItemData} subtree directly and
     * keeps the dominant land-use (largest {@code LandPurposeArea}) rather than the flat
     * last-wins the shared {@link XmlStream} would give.
     */
    private int loadParcels(Path zipPath) {
        List<CadastreParcelRow> batch = new ArrayList<>(BATCH_SIZE);
        int total = 0;
        try (var in = Files.newInputStream(zipPath); ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".xml")) {
                    continue;
                }
                XMLStreamReader reader = null;
                try {
                    reader = XmlStream.safeReader(nonClosing(zis));
                    while (reader.hasNext()) {
                        if (reader.next() == XMLStreamConstants.START_ELEMENT
                                && reader.getLocalName().equals(PARCEL_ITEM)) {
                            CadastreParcelRow row = readParcel(reader);
                            if (row != null) {
                                batch.add(row);
                                if (batch.size() == BATCH_SIZE) {
                                    repository.insertParcels(List.copyOf(batch));
                                    total += batch.size();
                                    batch.clear();
                                }
                            }
                        }
                    }
                } catch (XMLStreamException e) {
                    throw new UncheckedIOException(new IOException("Malformed cadastre parcel XML in " + entry.getName(), e));
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (XMLStreamException ignored) {
                            // reader.close() never touches the shared ZipInputStream (nonClosing wrapper)
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!batch.isEmpty()) {
            repository.insertParcels(List.copyOf(batch));
            total += batch.size();
        }
        return total;
    }

    /** Reads one {@code ParcelItemData} subtree (reader is positioned on its start tag), or null if it has no cadastre nr. */
    private static CadastreParcelRow readParcel(XMLStreamReader reader) throws XMLStreamException {
        String cadastreNr = null;
        BigDecimal parcelArea = null;
        String dominantPurposeId = null;
        BigDecimal dominantPurposeArea = null;
        String purposeId = null;
        BigDecimal purposeArea = null;

        int depth = 1;
        String leaf = null;
        StringBuilder text = new StringBuilder();
        while (depth > 0 && reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT -> {
                    depth++;
                    if (reader.getLocalName().equals(LAND_PURPOSE)) {
                        purposeId = null;
                        purposeArea = null;
                    }
                    leaf = reader.getLocalName();
                    text.setLength(0);
                }
                case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> {
                    if (leaf != null) {
                        text.append(reader.getText());
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    String name = reader.getLocalName();
                    if (leaf != null && name.equals(leaf)) {
                        String value = text.toString().trim();
                        switch (name) {
                            case "ParcelCadastreNr" -> cadastreNr = value;
                            case "ParcelArea" -> parcelArea = parseArea(value);
                            case "LandPurposeKindId" -> purposeId = value;
                            case "LandPurposeArea" -> purposeArea = parseArea(value);
                            default -> { /* other leaves ignored */ }
                        }
                    }
                    if (name.equals(LAND_PURPOSE) && purposeId != null && !purposeId.isBlank()
                            && (dominantPurposeId == null || greater(purposeArea, dominantPurposeArea))) {
                        dominantPurposeId = purposeId;
                        dominantPurposeArea = purposeArea;
                    }
                    leaf = null;
                    depth--;
                }
                default -> { /* ignore comments, whitespace-only events, etc. */ }
            }
        }
        if (cadastreNr == null || cadastreNr.isBlank()) {
            return null;
        }
        return new CadastreParcelRow(cadastreNr, parcelArea, mapLandUse(dominantPurposeId));
    }

    /** Whether {@code a} is a larger area than the current best {@code b} (a null area never displaces a known one). */
    private static boolean greater(BigDecimal a, BigDecimal b) {
        if (a == null) {
            return false;
        }
        return b == null || a.compareTo(b) > 0;
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

    /**
     * Maps a VZD NĪLM land-use code (nekustamā īpašuma lietošanas mērķis — the 4-digit
     * {@code LandPurposeKindId}) to our coarse {@link LandUse} bucket by its group prefix,
     * verified against the real dataset: {@code 01} agricultural, {@code 02} forest,
     * {@code 06}/{@code 07} residential (individual + multi-apartment housing), {@code 08}
     * commercial. Groups outside our four buckets ({@code 03} water, {@code 05} nature,
     * {@code 09} public, {@code 10} industrial, {@code 11} transport, {@code 12} utilities,
     * …) and absent values map to {@code null}, which makes {@code decideStatus} fail open
     * (no land-use hold).
     */
    private static LandUse mapLandUse(String kindId) {
        if (kindId == null || kindId.length() < 2) {
            return null;
        }
        return switch (kindId.substring(0, 2)) {
            case "01" -> LandUse.AGRICULTURAL;
            case "02" -> LandUse.FOREST;
            case "06", "07" -> LandUse.RESIDENTIAL;
            case "08" -> LandUse.COMMERCIAL;
            default -> null;
        };
    }
}
