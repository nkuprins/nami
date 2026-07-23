-- Migration: listings.cadastre_verified (run once against an existing database;
-- db/schema.sql already contains the final shape for fresh installs).
--
-- True when a listing's posted figures were positively matched against the VZD
-- cadastre mirror at create/edit time (see CadastreQueryService). Drives the
-- "verified against cadastre" badge. Existing rows default to false; they flip
-- to true the next time the owner edits/renews and the check passes.

ALTER TABLE listings
    ADD COLUMN cadastre_verified BOOLEAN NOT NULL DEFAULT false;
