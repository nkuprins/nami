-- ─────────────────────────────────────────────
-- Extensions
-- ─────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

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
    'new_building',
    'basement'
);
CREATE TYPE heating_type        AS ENUM ('central', 'gas', 'electric', 'heat_pump', 'solid_fuel', 'none');
-- Uppercase: canonical EU energy-performance-certificate letters, shown to users verbatim
CREATE TYPE energy_class        AS ENUM ('A', 'B', 'C', 'D', 'E', 'F', 'G');
CREATE TYPE bathroom_layout     AS ENUM ('separate', 'combined');

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
    email_verified BOOLEAN     NOT NULL DEFAULT false,
    last_login_at TIMESTAMPTZ NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Case-insensitive unique email: prevents user@example.com / User@Example.com duplicates
CREATE UNIQUE INDEX idx_users_email_lower ON users (lower(email));

-- ─────────────────────────────────────────────
-- Auth Tokens
-- ─────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT        NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

CREATE TABLE password_reset_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT        NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);

CREATE TABLE email_verification_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT        NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens(user_id);

-- ─────────────────────────────────────────────
-- Properties (address registry)
--
-- A thin, shared record of a physical address. Every physical/media attribute
-- lives on the listing (see below); a property only groups the listings that
-- sit at the same address (shared map pin, per-owner cap, duplicate guard).
-- ─────────────────────────────────────────────
CREATE TABLE properties (
    id                UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id          UUID              NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    -- Location: slugs validated at API layer; mapping owned by frontend locations.ts
    district_slug     TEXT              NOT NULL CHECK (district_slug ~ '^[a-z0-9-]+$'),
    city_slug         TEXT              NOT NULL CHECK (city_slug ~ '^[a-z0-9-]+$'),
    address           TEXT              NOT NULL CHECK (char_length(address) > 0),
    lat               DOUBLE PRECISION  NOT NULL,
    lng               DOUBLE PRECISION  NOT NULL,

    updated_at        TIMESTAMPTZ       NOT NULL DEFAULT now()
);

CREATE INDEX idx_properties_owner         ON properties (owner_id);
CREATE INDEX idx_properties_city          ON properties (city_slug);
CREATE INDEX idx_properties_district      ON properties (district_slug);
CREATE INDEX idx_properties_city_district ON properties (city_slug, district_slug);

-- ─────────────────────────────────────────────
-- Listings (market offer — self-contained)
-- ─────────────────────────────────────────────
CREATE TABLE listings (
    id                  UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id         UUID              NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    owner_id            UUID              NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    listing_type        listing_type      NOT NULL,
    price               NUMERIC(14, 2)    NOT NULL CHECK (price >= 0),
    vat_included        BOOLEAN           NOT NULL DEFAULT false,

    property_category   property_category NOT NULL,

    -- Physical dimensions
    rooms               SMALLINT          NOT NULL CHECK (rooms > 0),
    bedrooms            SMALLINT          CHECK (bedrooms >= 0 AND bedrooms <= rooms),
    bathrooms           SMALLINT          CHECK (bathrooms >= 0),
    bathroom_layout     bathroom_layout,
    m2                  NUMERIC(6, 2)     NOT NULL CHECK (m2 > 0),
    land_m2             NUMERIC(8, 2)     CHECK (land_m2 > 0),
    floor               SMALLINT          CHECK (floor >= 0),
    total_floors        SMALLINT          CHECK (total_floors > 0),
    year_built          SMALLINT          CHECK (year_built BETWEEN 1800 AND 2200),

    -- Building characteristics
    heating             heating_type,
    energy_class        energy_class,
    maintenance_cost    NUMERIC(10, 2)    CHECK (maintenance_cost >= 0),   -- monthly, EUR

    -- Media: ordered arrays of URL strings; array order is display order
    photos              JSONB             NOT NULL DEFAULT '[]',
    plans               JSONB             NOT NULL DEFAULT '[]',
    video_url           TEXT,

    -- new_project specific; 'not_ready → no year_built' rule enforced at service layer
    completion          property_completion,

    status              property_status   NOT NULL DEFAULT 'active',
    posted_at           TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ       NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ       NOT NULL,
    expiry_warning_sent BOOLEAN           NOT NULL DEFAULT false,

    -- Contact phones: ordered array of strings; array order is display order
    phones              JSONB             NOT NULL DEFAULT '[]',

    CONSTRAINT chk_land_m2_apartment
        CHECK (property_category != 'apartment' OR land_m2 IS NULL),
    CONSTRAINT chk_floor_requires_total
        CHECK (floor IS NULL OR total_floors IS NOT NULL),
    CONSTRAINT chk_floor_lte_total
        CHECK (floor IS NULL OR floor <= total_floors),
    CONSTRAINT chk_completion_new_project_only
        CHECK (listing_type = 'new_project' OR completion IS NULL)
);

CREATE INDEX idx_listings_type_status_price ON listings (listing_type, status, price);
CREATE INDEX idx_listings_owner             ON listings (owner_id);
CREATE INDEX idx_listings_property          ON listings (property_id);
CREATE INDEX idx_listings_posted_at         ON listings (posted_at DESC);
-- Expiry job: find active listings that are expiring
CREATE INDEX idx_listings_expires_at ON listings (expires_at) WHERE status = 'active';

-- ─────────────────────────────────────────────
-- Listing features
-- ─────────────────────────────────────────────
CREATE TABLE listing_features (
    listing_id UUID             NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    feature    property_feature NOT NULL,
    PRIMARY KEY (listing_id, feature)
);

CREATE INDEX idx_listing_features_feature ON listing_features (feature);

-- ─────────────────────────────────────────────
-- Listing translations
-- ─────────────────────────────────────────────
CREATE TABLE listing_translations (
    listing_id  UUID NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    locale      TEXT NOT NULL CHECK (locale IN ('lv', 'en', 'ru')),
    title       TEXT NOT NULL CHECK (char_length(title) > 0),
    description TEXT NOT NULL CHECK (char_length(description) > 0),
    PRIMARY KEY (listing_id, locale)
);

CREATE INDEX idx_listing_translations_listing ON listing_translations (listing_id);

-- ─────────────────────────────────────────────
-- Saved listings
-- ─────────────────────────────────────────────
CREATE TABLE saved_listings (
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    listing_id UUID        NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    saved_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, listing_id)
);

CREATE INDEX idx_saved_listings_user    ON saved_listings (user_id);
CREATE INDEX idx_saved_listings_listing ON saved_listings (listing_id);

-- ─────────────────────────────────────────────
-- Pending media deletions (S3 cleanup)
-- ─────────────────────────────────────────────
CREATE TABLE pending_media_deletions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    cdn_url     TEXT        NOT NULL,
    attempts    INT         NOT NULL DEFAULT 0,
    last_error  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────────
-- updated_at triggers
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

CREATE TRIGGER trg_listings_updated_at
    BEFORE UPDATE ON listings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
