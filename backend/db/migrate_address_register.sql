-- Migration: State Address Register integration (run once against an existing
-- database; db/schema.sql already contains the final shape for fresh installs).
--
-- 1. New register mirror tables (populated by AddressRegistryIngestService).
-- 2. Structured-address columns on properties. Existing rows keep their
--    free-text address with NULL ar_building_code (legacy).

CREATE TABLE address_territories (
    code             BIGINT   PRIMARY KEY,
    type_cd          SMALLINT NOT NULL,
    name             TEXT     NOT NULL,
    norm_name        TEXT     NOT NULL,
    novads_name      TEXT,
    norm_novads_name TEXT
);

CREATE INDEX idx_address_territories_norm_name   ON address_territories (norm_name);
CREATE INDEX idx_address_territories_norm_novads ON address_territories (norm_novads_name);

CREATE TABLE address_streets (
    code           BIGINT PRIMARY KEY,
    territory_code BIGINT NOT NULL REFERENCES address_territories (code) ON DELETE CASCADE,
    name           TEXT   NOT NULL,
    norm_name      TEXT   NOT NULL
);

CREATE INDEX idx_address_streets_territory_norm ON address_streets (territory_code, norm_name text_pattern_ops);

CREATE TABLE address_buildings (
    code           BIGINT PRIMARY KEY,
    street_code    BIGINT REFERENCES address_streets (code) ON DELETE CASCADE,
    territory_code BIGINT NOT NULL REFERENCES address_territories (code) ON DELETE CASCADE,
    name           TEXT   NOT NULL,
    norm_name      TEXT   NOT NULL,
    lat            DOUBLE PRECISION,
    lng            DOUBLE PRECISION
);

CREATE INDEX idx_address_buildings_street_norm    ON address_buildings (street_code, norm_name text_pattern_ops);
CREATE INDEX idx_address_buildings_territory_norm ON address_buildings (territory_code, norm_name text_pattern_ops)
    WHERE street_code IS NULL;

ALTER TABLE properties
    ADD COLUMN ar_building_code BIGINT,
    ADD COLUMN apartment        TEXT;
