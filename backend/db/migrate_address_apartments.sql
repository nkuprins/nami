-- Migration: mirror VZD's apartment-level address codes (aw_dziv.csv), run
-- once against an existing database; db/schema.sql already contains the final
-- shape for fresh installs.
--
-- Links a listing's free-typed apartment number to its own VAR address code,
-- which the cadastre mirror's premise groups reference by
-- PremiseGroupVARISCode (see migrate_cadastre_register.sql).

CREATE TABLE address_apartments (
    code           BIGINT PRIMARY KEY,
    building_code  BIGINT NOT NULL REFERENCES address_buildings (code) ON DELETE CASCADE,
    name           TEXT   NOT NULL,
    norm_name      TEXT   NOT NULL
);

CREATE INDEX idx_address_apartments_building_norm ON address_apartments (building_code, norm_name);
