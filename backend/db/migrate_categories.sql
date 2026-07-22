-- Migration: real-estate categories (commercial, land, garage) + new_project remodel.
-- Run ONCE against an existing database; db/schema.sql already holds the final shape
-- for fresh installs.
--
-- IMPORTANT: run WITHOUT a wrapping transaction (plain `psql -f`, NOT
-- `--single-transaction`). Postgres forbids using an enum value in the same
-- transaction that added it, so the ADD VALUE statements below must commit before
-- the backfill can reference the new labels.
--
-- What it does:
--   1. Adds the 4 new property_category values + commercial_type / land_use enums.
--   2. Adds the new sub-type / land-use columns and the parcel link; relaxes
--      rooms/m2 NOT NULL.
--   3. Backfills existing new_project listings onto the category axis.
--   4. Drops 'new_project' from listing_type via a type swap (fails loud if any
--      new_project row survived the backfill).
--   5. Re-scopes the CHECK constraints to the new model.
--   6. Adds the cadastre_parcels mirror table.

-- ── Step 1: new enum values / types (each auto-commits; must precede any use) ──
ALTER TYPE property_category ADD VALUE IF NOT EXISTS 'new_project';
ALTER TYPE property_category ADD VALUE IF NOT EXISTS 'commercial';
ALTER TYPE property_category ADD VALUE IF NOT EXISTS 'land';
ALTER TYPE property_category ADD VALUE IF NOT EXISTS 'garage';

CREATE TYPE commercial_type AS ENUM ('office', 'warehouse', 'retail', 'industrial', 'hospitality');
CREATE TYPE land_use        AS ENUM ('residential', 'commercial', 'agricultural', 'forest');

-- ── Step 2: new columns + NOT NULL relaxation ──
ALTER TABLE listings
    ADD COLUMN new_project_kind   property_category,
    ADD COLUMN commercial_subtype commercial_type,
    ADD COLUMN land_use           land_use;

ALTER TABLE listings ALTER COLUMN rooms DROP NOT NULL;
ALTER TABLE listings ALTER COLUMN m2    DROP NOT NULL;

ALTER TABLE properties
    ADD COLUMN cadastre_parcel_nr TEXT;

-- ── Step 3: backfill existing new_project listings onto the category axis ──
-- Old model: listing_type='new_project' + property_category(apartment|house).
-- New model: property_category='new_project', kind preserved as the subtype,
-- transaction defaults to 'buy' (new projects are for-sale; original intent is
-- unrecoverable — this is the only defensible default).
UPDATE listings
SET new_project_kind  = property_category,
    property_category = 'new_project',
    listing_type      = 'buy'
WHERE listing_type = 'new_project';

-- ── Step 4: drop the old constraints that reference the soon-to-vanish enum value,
--            then swap listing_type down to ('buy','rent') ──
ALTER TABLE listings DROP CONSTRAINT chk_completion_new_project_only;
ALTER TABLE listings DROP CONSTRAINT chk_land_m2_apartment;

CREATE TYPE listing_type_new AS ENUM ('buy', 'rent');
-- USING cast fails loudly if any 'new_project' row survived the backfill — a safety net.
ALTER TABLE listings
    ALTER COLUMN listing_type TYPE listing_type_new
    USING (listing_type::text::listing_type_new);
DROP TYPE listing_type;
ALTER TYPE listing_type_new RENAME TO listing_type;

-- ── Step 5: re-scope CHECK constraints to the new model ──
ALTER TABLE listings
    ADD CONSTRAINT chk_land_m2_scope
        CHECK (property_category IN ('house', 'land') OR land_m2 IS NULL),
    ADD CONSTRAINT chk_completion_new_project_only
        CHECK (property_category = 'new_project' OR completion IS NULL),
    ADD CONSTRAINT chk_new_project_kind
        CHECK ((property_category = 'new_project') = (new_project_kind IS NOT NULL)
               AND (new_project_kind IS NULL OR new_project_kind IN ('apartment', 'house'))),
    ADD CONSTRAINT chk_commercial_subtype
        CHECK ((property_category = 'commercial') = (commercial_subtype IS NOT NULL)),
    ADD CONSTRAINT chk_land_use_scope
        CHECK (land_use IS NULL OR property_category IN ('land', 'commercial')),
    ADD CONSTRAINT chk_rooms_scope
        CHECK (property_category IN ('apartment', 'house', 'new_project', 'commercial')
               OR rooms IS NULL);

-- ── Step 6: cadastre parcel mirror ──
CREATE TABLE cadastre_parcels (
    cadastre_nr TEXT PRIMARY KEY,
    area_m2     NUMERIC(12, 2),
    land_use    land_use
);

CREATE INDEX idx_cadastre_parcels_land_use ON cadastre_parcels (land_use);
