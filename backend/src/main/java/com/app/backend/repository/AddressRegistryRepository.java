package com.app.backend.repository;

import com.app.backend.dto.address.BuildingAddress;
import com.app.backend.dto.address.BuildingOptionDto;
import com.app.backend.dto.address.BuildingRow;
import com.app.backend.dto.address.StreetOptionDto;
import com.app.backend.dto.address.StreetRow;
import com.app.backend.dto.address.TerritoryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plain-JDBC access to the State Address Register mirror tables. The tables are
 * wiped and reloaded weekly by the ingest service, so they are deliberately not
 * JPA entities and carry no foreign keys from user data.
 */
@Repository
@RequiredArgsConstructor
public class AddressRegistryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    // ── ingest ────────────────────────────────────────────────

    public int countTerritories() {
        Integer count = jdbc.getJdbcTemplate()
                .queryForObject("SELECT count(*) FROM address_territories", Integer.class);
        return count != null ? count : 0;
    }

    public void deleteAll() {
        jdbc.getJdbcTemplate().update("DELETE FROM address_buildings");
        jdbc.getJdbcTemplate().update("DELETE FROM address_streets");
        jdbc.getJdbcTemplate().update("DELETE FROM address_territories");
    }

    public void insertTerritories(List<TerritoryRow> rows) {
        jdbc.getJdbcTemplate().batchUpdate("""
                INSERT INTO address_territories (code, type_cd, name, norm_name, novads_name, norm_novads_name)
                VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (code) DO NOTHING
                """, rows, rows.size(), (PreparedStatement ps, TerritoryRow r) -> {
            ps.setLong(1, r.code());
            ps.setInt(2, r.typeCd());
            ps.setString(3, r.name());
            ps.setString(4, r.normName());
            ps.setString(5, r.novadsName());
            ps.setString(6, r.normNovadsName());
        });
    }

    public void insertStreets(List<StreetRow> rows) {
        jdbc.getJdbcTemplate().batchUpdate("""
                INSERT INTO address_streets (code, territory_code, name, norm_name)
                VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING
                """, rows, rows.size(), (PreparedStatement ps, StreetRow r) -> {
            ps.setLong(1, r.code());
            ps.setLong(2, r.territoryCode());
            ps.setString(3, r.name());
            ps.setString(4, r.normName());
        });
    }

    public void insertBuildings(List<BuildingRow> rows) {
        jdbc.getJdbcTemplate().batchUpdate("""
                INSERT INTO address_buildings (code, street_code, territory_code, name, norm_name, lat, lng)
                VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (code) DO NOTHING
                """, rows, rows.size(), (PreparedStatement ps, BuildingRow r) -> {
            ps.setLong(1, r.code());
            if (r.streetCode() != null) {
                ps.setLong(2, r.streetCode());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setLong(3, r.territoryCode());
            ps.setString(4, r.name());
            ps.setString(5, r.normName());
            if (r.lat() != null) {
                ps.setDouble(6, r.lat());
            } else {
                ps.setNull(6, Types.DOUBLE);
            }
            if (r.lng() != null) {
                ps.setDouble(7, r.lng());
            } else {
                ps.setNull(7, Types.DOUBLE);
            }
        });
    }

    // ── queries ───────────────────────────────────────────────

    public List<TerritoryRow> findTerritoriesByNormName(String normName) {
        return jdbc.query("""
                SELECT code, type_cd, name, norm_name, novads_name, norm_novads_name
                FROM address_territories WHERE norm_name = :name
                """, Map.of("name", normName), (rs, i) -> new TerritoryRow(
                rs.getLong("code"), rs.getInt("type_cd"), rs.getString("name"),
                rs.getString("norm_name"), rs.getString("novads_name"), rs.getString("norm_novads_name")));
    }

    /** Streets in the given territories whose normalized name starts with (or has a word starting with) the query. */
    public List<StreetOptionDto> searchStreets(Collection<Long> territoryCodes, String normQuery, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("codes", territoryCodes)
                .addValue("prefix", normQuery + "%")
                .addValue("word", "% " + normQuery + "%")
                .addValue("limit", limit);
        return jdbc.query("""
                SELECT s.code, s.name, t.name AS territory
                FROM address_streets s
                JOIN address_territories t ON t.code = s.territory_code
                WHERE s.territory_code IN (:codes)
                  AND (s.norm_name LIKE :prefix OR s.norm_name LIKE :word)
                ORDER BY (s.norm_name LIKE :prefix) DESC, s.norm_name, s.code
                LIMIT :limit
                """, params, (rs, i) -> StreetOptionDto.street(
                rs.getLong("code"), rs.getString("name"), rs.getString("territory")));
    }

    /** Rural houses named directly under one of the territories (no street). */
    public List<StreetOptionDto> searchTerritoryHouses(Collection<Long> territoryCodes, String normQuery, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("codes", territoryCodes)
                .addValue("prefix", normQuery + "%")
                .addValue("limit", limit);
        return jdbc.query("""
                SELECT b.code, b.name, t.name AS territory, b.lat, b.lng
                FROM address_buildings b
                JOIN address_territories t ON t.code = b.territory_code
                WHERE b.street_code IS NULL
                  AND b.territory_code IN (:codes)
                  AND b.norm_name LIKE :prefix
                ORDER BY b.norm_name, b.code
                LIMIT :limit
                """, params, (rs, i) -> StreetOptionDto.house(
                rs.getLong("code"), rs.getString("name"), rs.getString("territory"),
                nullableDouble(rs.getObject("lat")), nullableDouble(rs.getObject("lng"))));
    }

    /** House numbers under a street, shortest (numerically smallest) first. */
    public List<BuildingOptionDto> searchBuildings(long streetCode, String normQuery, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("street", streetCode)
                .addValue("prefix", normQuery + "%")
                .addValue("limit", limit);
        return jdbc.query("""
                SELECT code, name, lat, lng
                FROM address_buildings
                WHERE street_code = :street AND norm_name LIKE :prefix
                ORDER BY length(norm_name), norm_name, code
                LIMIT :limit
                """, params, (rs, i) -> new BuildingOptionDto(
                rs.getLong("code"), rs.getString("name"),
                nullableDouble(rs.getObject("lat")), nullableDouble(rs.getObject("lng"))));
    }

    public Optional<BuildingAddress> findBuildingAddress(long code) {
        List<BuildingAddress> found = jdbc.query("""
                SELECT b.code, b.name, s.name AS street_name, t.name AS territory_name, b.lat, b.lng
                FROM address_buildings b
                LEFT JOIN address_streets s ON s.code = b.street_code
                JOIN address_territories t ON t.code = b.territory_code
                WHERE b.code = :code
                """, Map.of("code", code), (rs, i) -> new BuildingAddress(
                rs.getLong("code"), rs.getString("name"), rs.getString("street_name"),
                rs.getString("territory_name"),
                nullableDouble(rs.getObject("lat")), nullableDouble(rs.getObject("lng"))));
        return found.stream().findFirst();
    }

    private static Double nullableDouble(Object value) {
        return value instanceof Number n ? n.doubleValue() : null;
    }
}
