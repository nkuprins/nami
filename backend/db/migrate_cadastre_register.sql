-- Migration: VZD Cadastre mirror (run once against an existing database;
-- db/schema.sql already contains the final shape for fresh installs).
--
-- Cross-checks a listing's posted area/build year against the official
-- cadastre record at creation and edit time (see CadastreQueryService). No
-- FKs to the address_* mirror: both are wiped and reloaded independently.

CREATE TABLE cadastre_buildings (
    cadastre_nr      TEXT PRIMARY KEY,
    ar_building_code BIGINT,
    year_built       SMALLINT
);

CREATE INDEX idx_cadastre_buildings_ar_code ON cadastre_buildings (ar_building_code);

CREATE TABLE cadastre_premises (
    cadastre_nr TEXT PRIMARY KEY,
    ar_code     BIGINT,
    area_m2     NUMERIC(8, 2)
);

CREATE INDEX idx_cadastre_premises_ar_code ON cadastre_premises (ar_code);
