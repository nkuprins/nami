-- ─────────────────────────────────────────────────────────────────────────────
-- Migration: flatten the physical/media attributes from `properties` down onto
-- `listings` (2-tier "self-contained listing" model). `properties` becomes a
-- shared address registry; `property_features` becomes `listing_features`.
--
-- DO NOT auto-run. `db/schema.sql` is authoritative for fresh databases (and the
-- Testcontainers test suite); this script exists ONLY to migrate the already
-- deployed Neon database in place. Apply it manually, once, after taking a
-- backup.
--
-- Baseline assumption: the deployed schema is the PRE-override shared model —
-- `properties` still holds the physical/media columns, `listings` references
-- `property_id` with UNIQUE(property_id, listing_type), and features live in
-- `property_features`. The `ovr_*` override columns were never committed, so
-- they are NOT expected here. Confirm the live schema before running:
--     \d properties     \d listings     \d property_features
-- If the live DB differs, adjust before applying.
--
-- Each existing property has 1–N listings; every listing gets its own COPY of the
-- property's physical/media/features (duplication is intentional in this model).
-- ─────────────────────────────────────────────────────────────────────────────

BEGIN;

-- 1. New self-contained columns on listings (nullable/defaulted for backfill).
ALTER TABLE listings
    ADD COLUMN property_category property_category,
    ADD COLUMN rooms            SMALLINT,
    ADD COLUMN bedrooms         SMALLINT,
    ADD COLUMN bathrooms        SMALLINT,
    ADD COLUMN bathroom_layout  bathroom_layout,
    ADD COLUMN m2               NUMERIC(6, 2),
    ADD COLUMN land_m2          NUMERIC(8, 2),
    ADD COLUMN floor            SMALLINT,
    ADD COLUMN total_floors     SMALLINT,
    ADD COLUMN year_built       SMALLINT,
    ADD COLUMN heating          heating_type,
    ADD COLUMN energy_class     energy_class,
    ADD COLUMN maintenance_cost NUMERIC(10, 2),
    ADD COLUMN photos           JSONB NOT NULL DEFAULT '[]',
    ADD COLUMN plans            JSONB NOT NULL DEFAULT '[]',
    ADD COLUMN video_url        TEXT;

-- 2. listing_features junction.
CREATE TABLE listing_features (
    listing_id UUID             NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    feature    property_feature NOT NULL,
    PRIMARY KEY (listing_id, feature)
);
CREATE INDEX idx_listing_features_feature ON listing_features (feature);

-- 3. Backfill each listing from its parent property.
UPDATE listings l SET
    property_category = p.property_category,
    rooms             = p.rooms,
    bedrooms          = p.bedrooms,
    bathrooms         = p.bathrooms,
    bathroom_layout   = p.bathroom_layout,
    m2                = p.m2,
    land_m2           = p.land_m2,
    floor             = p.floor,
    total_floors      = p.total_floors,
    year_built        = p.year_built,
    heating           = p.heating,
    energy_class      = p.energy_class,
    maintenance_cost  = p.maintenance_cost,
    photos            = p.photos,
    plans             = p.plans,
    video_url         = p.video_url
FROM properties p
WHERE l.property_id = p.id;

INSERT INTO listing_features (listing_id, feature)
SELECT l.id, pf.feature
FROM property_features pf
JOIN listings l ON l.property_id = pf.property_id
ON CONFLICT DO NOTHING;

-- 4. Enforce the invariants now that the data is in place.
ALTER TABLE listings
    ALTER COLUMN property_category SET NOT NULL,
    ALTER COLUMN rooms SET NOT NULL,
    ALTER COLUMN m2 SET NOT NULL,
    ADD CONSTRAINT chk_land_m2_apartment
        CHECK (property_category != 'apartment' OR land_m2 IS NULL),
    ADD CONSTRAINT chk_floor_requires_total
        CHECK (floor IS NULL OR total_floors IS NOT NULL),
    ADD CONSTRAINT chk_floor_lte_total
        CHECK (floor IS NULL OR floor <= total_floors);
-- (rooms > 0, bedrooms <= rooms, etc. carry over from the source data.)

-- 5. Unlimited listings per type per address — drop the old uniqueness guard.
ALTER TABLE listings DROP CONSTRAINT IF EXISTS uq_listings_property_type;

-- 6. Retire the old feature junction and the moved property columns.
DROP TABLE property_features;

ALTER TABLE properties
    DROP COLUMN property_category,
    DROP COLUMN rooms,
    DROP COLUMN bedrooms,
    DROP COLUMN bathrooms,
    DROP COLUMN bathroom_layout,
    DROP COLUMN m2,
    DROP COLUMN land_m2,
    DROP COLUMN floor,
    DROP COLUMN total_floors,
    DROP COLUMN year_built,
    DROP COLUMN heating,
    DROP COLUMN energy_class,
    DROP COLUMN maintenance_cost,
    DROP COLUMN photos,
    DROP COLUMN plans,
    DROP COLUMN video_url;

COMMIT;
