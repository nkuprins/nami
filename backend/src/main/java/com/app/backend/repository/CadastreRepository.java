package com.app.backend.repository;

import com.app.backend.dto.cadastre.CadastreBuildingRow;
import com.app.backend.dto.cadastre.CadastreParcelRow;
import com.app.backend.dto.cadastre.CadastrePremiseRow;
import com.app.backend.enums.LandUse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plain-JDBC access to the VZD cadastre mirror tables. Wiped and reloaded by
 * the ingest service, so these are deliberately not JPA entities and carry no
 * foreign keys from user data or the address-register mirror.
 */
@Repository
@RequiredArgsConstructor
public class CadastreRepository {

    private final NamedParameterJdbcTemplate jdbc;

    // ── ingest ────────────────────────────────────────────────

    public int countBuildings() {
        Integer count = jdbc.getJdbcTemplate()
                .queryForObject("SELECT count(*) FROM cadastre_buildings", Integer.class);
        return count != null ? count : 0;
    }

    public void deleteAll() {
        jdbc.getJdbcTemplate().update("DELETE FROM cadastre_premises");
        jdbc.getJdbcTemplate().update("DELETE FROM cadastre_buildings");
        jdbc.getJdbcTemplate().update("DELETE FROM cadastre_parcels");
    }

    public int countParcels() {
        Integer count = jdbc.getJdbcTemplate()
                .queryForObject("SELECT count(*) FROM cadastre_parcels", Integer.class);
        return count != null ? count : 0;
    }

    public void insertParcels(List<CadastreParcelRow> rows) {
        jdbc.getJdbcTemplate().batchUpdate("""
                INSERT INTO cadastre_parcels (cadastre_nr, area_m2, land_use)
                VALUES (?, ?, ?::land_use) ON CONFLICT (cadastre_nr) DO NOTHING
                """, rows, rows.size(), (PreparedStatement ps, CadastreParcelRow r) -> {
            ps.setString(1, r.cadastreNr());
            if (r.areaM2() != null) {
                ps.setBigDecimal(2, r.areaM2());
            } else {
                ps.setNull(2, Types.NUMERIC);
            }
            if (r.landUse() != null) {
                ps.setString(3, r.landUse().getDbValue());
            } else {
                ps.setNull(3, Types.OTHER);
            }
        });
    }

    public void insertBuildings(List<CadastreBuildingRow> rows) {
        jdbc.getJdbcTemplate().batchUpdate("""
                INSERT INTO cadastre_buildings (cadastre_nr, ar_building_code, year_built)
                VALUES (?, ?, ?) ON CONFLICT (cadastre_nr) DO NOTHING
                """, rows, rows.size(), (PreparedStatement ps, CadastreBuildingRow r) -> {
            ps.setString(1, r.cadastreNr());
            if (r.arBuildingCode() != null) {
                ps.setLong(2, r.arBuildingCode());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            if (r.yearBuilt() != null) {
                ps.setShort(3, r.yearBuilt());
            } else {
                ps.setNull(3, Types.SMALLINT);
            }
        });
    }

    public void insertPremises(List<CadastrePremiseRow> rows) {
        jdbc.getJdbcTemplate().batchUpdate("""
                INSERT INTO cadastre_premises (cadastre_nr, ar_code, area_m2)
                VALUES (?, ?, ?) ON CONFLICT (cadastre_nr) DO NOTHING
                """, rows, rows.size(), (PreparedStatement ps, CadastrePremiseRow r) -> {
            ps.setString(1, r.cadastreNr());
            if (r.arCode() != null) {
                ps.setLong(2, r.arCode());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            if (r.areaM2() != null) {
                ps.setBigDecimal(3, r.areaM2());
            } else {
                ps.setNull(3, Types.NUMERIC);
            }
        });
    }

    // ── queries ───────────────────────────────────────────────

    public Optional<Short> findYearBuiltByArCode(long arBuildingCode) {
        List<Short> found = jdbc.query("""
                SELECT year_built FROM cadastre_buildings WHERE ar_building_code = :code AND year_built IS NOT NULL
                """, Map.of("code", arBuildingCode), (rs, i) -> rs.getShort("year_built"));
        return found.stream().findFirst();
    }

    public Optional<BigDecimal> findAreaByArCode(long arCode) {
        List<BigDecimal> found = jdbc.query("""
                SELECT area_m2 FROM cadastre_premises WHERE ar_code = :code AND area_m2 IS NOT NULL
                """, Map.of("code", arCode), (rs, i) -> rs.getBigDecimal("area_m2"));
        return found.stream().findFirst();
    }

    public Optional<CadastreParcelRow> findParcelByCadastreNr(String cadastreNr) {
        List<CadastreParcelRow> found = jdbc.query("""
                SELECT cadastre_nr, area_m2, land_use FROM cadastre_parcels WHERE cadastre_nr = :nr
                """, Map.of("nr", cadastreNr), (rs, i) -> {
            String landUse = rs.getString("land_use");
            return new CadastreParcelRow(
                    rs.getString("cadastre_nr"),
                    rs.getBigDecimal("area_m2"),
                    landUse != null ? LandUse.fromDbValue(landUse) : null);
        });
        return found.stream().findFirst();
    }
}
