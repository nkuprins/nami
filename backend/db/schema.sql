-- ─────────────────────────────────────────────
-- Extensions
-- ─────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "cube";
CREATE EXTENSION IF NOT EXISTS "earthdistance";

-- ─────────────────────────────────────────────
-- Enum types
-- ─────────────────────────────────────────────
CREATE TYPE listing_type        AS ENUM ('buy', 'rent', 'new_project');
CREATE TYPE property_category   AS ENUM ('apartment', 'house');
CREATE TYPE property_completion AS ENUM ('ready', 'not_ready');
CREATE TYPE property_status     AS ENUM ('active', 'inactive');
CREATE TYPE property_feature    AS ENUM (
    'balcony',
    'parking',
    'elevator',
    'furnished',
    'pets',
    'new_building'
);

-- ─────────────────────────────────────────────
-- Users
-- ─────────────────────────────────────────────
CREATE TABLE users (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name          TEXT        NOT NULL,
    email         TEXT        NOT NULL UNIQUE
                              CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    password_hash TEXT        NOT NULL
                              CHECK (char_length(password_hash) >= 60),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Case-insensitive unique email: prevents user@example.com / User@Example.com duplicates
CREATE UNIQUE INDEX idx_users_email_lower ON users (lower(email));

-- ─────────────────────────────────────────────
-- Properties
-- ─────────────────────────────────────────────
CREATE TABLE properties (
    id                UUID                 PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id          UUID                 NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    -- Listing metadata
    listing_type      listing_type         NOT NULL,
    property_category property_category    NOT NULL,
	status            property_status      NOT NULL DEFAULT 'active',
    title             TEXT                 NOT NULL,
    description       TEXT                 NOT NULL DEFAULT '',

    -- Pricing
    price             NUMERIC(14, 2)       NOT NULL CHECK (price >= 0),

    -- Physical attributes
    rooms             SMALLINT             NOT NULL CHECK (rooms > 0),
    m2                NUMERIC(6, 2)        NOT NULL CHECK (m2 > 0),
    land_m2           NUMERIC(8, 2)        CHECK (land_m2 > 0),
    floor             SMALLINT             CHECK (floor >= 0),  -- 0 = ground floor (EU convention)
    total_floors      SMALLINT             CHECK (total_floors > 0),
    year_built        SMALLINT             CHECK (year_built BETWEEN 1800 AND 2200),

    -- new_project specific
    completion        property_completion,

    -- Location: slugs validated at API layer; mapping owned by frontend locations.ts
    district_slug     TEXT                 NOT NULL CHECK (district_slug ~ '^[a-z0-9-]+$'),
    city_slug         TEXT                 NOT NULL CHECK (city_slug ~ '^[a-z0-9-]+$'),
    address           TEXT                 NOT NULL CHECK (char_length(address) > 0),
    lat               DOUBLE PRECISION     NOT NULL,
    lng               DOUBLE PRECISION     NOT NULL,

    -- Cross-column constraints
    CONSTRAINT chk_land_m2_apartment
        CHECK (property_category != 'apartment' OR land_m2 IS NULL),

    -- If floor is provided, total_floors must also be provided
    CONSTRAINT chk_floor_requires_total
        CHECK (floor IS NULL OR total_floors IS NOT NULL),
    CONSTRAINT chk_floor_lte_total
        CHECK (floor IS NULL OR floor <= total_floors),

	-- completion field is only valid on new_project listings
    CONSTRAINT chk_completion_new_project_only
        CHECK (listing_type = 'new_project' OR completion IS NULL),
	-- completion field when 'not_ready' can not have year_built
  	CONSTRAINT chk_year_built_not_ready
        CHECK (completion IS DISTINCT FROM 'not_ready' OR year_built IS NULL),

    -- Timestamps
    posted_at         TIMESTAMPTZ          NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ          NOT NULL DEFAULT now()
);

CREATE INDEX idx_properties_listing_type          ON properties (listing_type);
CREATE INDEX idx_properties_district              ON properties (district_slug);
CREATE INDEX idx_properties_city                  ON properties (city_slug);
CREATE INDEX idx_properties_price                 ON properties (price);
CREATE INDEX idx_properties_posted_at             ON properties (posted_at DESC);
CREATE INDEX idx_properties_owner                 ON properties (owner_id);
-- Covers the primary browse query: type + city + price range
CREATE INDEX idx_properties_type_city_price
    ON properties (listing_type, city_slug, price);
-- Covers city-scoped browsing sorted by recency (no type filter)
CREATE INDEX idx_properties_city_posted_at
    ON properties (city_slug, posted_at DESC);
-- Geospatial proximity (map view)
CREATE INDEX idx_properties_coords
    ON properties USING gist (ll_to_earth(lat, lng));

-- ─────────────────────────────────────────────
-- Property features
-- ─────────────────────────────────────────────
CREATE TABLE property_features (
    property_id UUID             NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    feature     property_feature NOT NULL,
    PRIMARY KEY (property_id, feature)
);

-- Supports "find all properties with feature X" queries
CREATE INDEX idx_property_features_feature ON property_features (feature);

-- ─────────────────────────────────────────────
-- Property photos
-- ─────────────────────────────────────────────
CREATE TABLE property_photos (
    id          UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID     NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    url         TEXT     NOT NULL CHECK (url ~ '^https?://'),
    position    SMALLINT NOT NULL DEFAULT 0 CHECK (position >= 0),
    -- DEFERRABLE so position swaps within a transaction don't violate uniqueness mid-flight
    UNIQUE (property_id, position) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_property_photos_property ON property_photos (property_id, position);

-- ─────────────────────────────────────────────
-- Saved listings
-- ─────────────────────────────────────────────
CREATE TABLE saved_properties (
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    property_id UUID        NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    saved_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, property_id)
);

CREATE INDEX idx_saved_properties_user     ON saved_properties (user_id);
-- Required for efficient cascade delete from properties
CREATE INDEX idx_saved_properties_property ON saved_properties (property_id);

-- ─────────────────────────────────────────────
-- updated_at trigger
-- ─────────────────────────────────────────────
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_properties_updated_at
    BEFORE UPDATE ON properties
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();