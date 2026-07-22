-- Migration: denormalize the register street onto properties (run once against
-- an existing database; db/schema.sql already contains the final shape for
-- fresh installs).
--
-- Adds properties.ar_street_code so listing search can filter by street without
-- joining the register mirror. New/edited listings populate it via
-- PropertyService.resolveLocation; the backfill below covers existing rows.

ALTER TABLE properties ADD COLUMN ar_street_code BIGINT;
CREATE INDEX idx_properties_ar_street_code ON properties (ar_street_code);

-- One-time backfill. Must run AFTER the address register has been ingested —
-- on an empty address_buildings this is a no-op and must be re-run once the
-- mirror is populated. Rows whose building has no street (rural houses) or no
-- ar_building_code (legacy free-text addresses) are correctly left NULL.
UPDATE properties p
   SET ar_street_code = b.street_code
  FROM address_buildings b
 WHERE b.code = p.ar_building_code
   AND b.street_code IS NOT NULL;
