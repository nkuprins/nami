-- ─────────────────────────────────────────────
-- Extensions
-- ─────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─────────────────────────────────────────────
-- Enum types
-- ─────────────────────────────────────────────
CREATE TYPE listing_type        AS ENUM ('buy', 'rent');
CREATE TYPE property_category   AS ENUM ('apartment', 'house', 'new_project', 'commercial', 'land', 'garage');
CREATE TYPE commercial_type     AS ENUM ('office', 'warehouse', 'retail', 'industrial', 'hospitality');
CREATE TYPE land_use            AS ENUM ('residential', 'commercial', 'agricultural', 'forest');
CREATE TYPE property_completion AS ENUM ('ready', 'not_ready');
CREATE TYPE property_status     AS ENUM ('active', 'inactive', 'pending_review');
CREATE TYPE user_role           AS ENUM ('user', 'admin');
CREATE TYPE property_feature    AS ENUM (
    'balcony',
    'parking',
    'elevator',
    'furnished',
    'pets',
    'new_building',
    'basement',
    'renovated',
    'air_conditioning',
    'terrace',
    'sauna',
    'fireplace',
    'underfloor_heating',
    'individual_meters',
    'storage_room',
    'walk_in_closet',
    'pool',
    'bathtub',
    'shower',
    'washing_machine',
    'boiler',
    'glazed_balcony',
    'french_balcony',
    'loggia'
);
CREATE TYPE heating_type        AS ENUM (
    'central', 'central_gas', 'gas', 'electric', 'heat_pump', 'air_water_heat_pump',
    'geothermal', 'solid_fuel', 'stove', 'combined', 'none'
);
-- Uppercase: canonical EU energy-performance-certificate letters, shown to users verbatim
CREATE TYPE energy_class        AS ENUM ('A', 'B', 'C', 'D', 'E', 'F', 'G');
CREATE TYPE bathroom_layout     AS ENUM ('separate', 'combined');
CREATE TYPE sewage_type         AS ENUM ('central', 'local');
CREATE TYPE ventilation_type    AS ENUM ('natural', 'mechanical', 'recuperation');
CREATE TYPE roof_type           AS ENUM (
    'bitumen', 'eternit', 'pvc', 'roll_material', 'steel', 'stone', 'tile', 'white_tin', 'zinc_plate'
);
CREATE TYPE ventilation_system  AS ENUM ('climate_control', 'supply_ventilation', 'air_conditioner');
CREATE TYPE communication_type  AS ENUM ('cable_tv', 'internet', 'telephone', 'digital_tv');
CREATE TYPE stove_type          AS ENUM ('electric_stove', 'wood_burning', 'gas_stove');
CREATE TYPE security_feature    AS ENUM (
    'locking_entrance', 'guard', 'security_system', 'steel_door', 'video_cameras'
);
CREATE TYPE property_extra      AS ENUM (
    'separate_entrance', 'enclosed_yard', 'private_garden', 'furniture', 'furniture_possible'
);
CREATE TYPE parking_type        AS ENUM (
    'free_parking', 'paid_parking', 'no_parking', 'underground_parking', 'own_parking_space'
);

-- ─────────────────────────────────────────────
-- Users
-- ─────────────────────────────────────────────
CREATE TABLE users (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name          TEXT        NOT NULL,
    email         TEXT        NOT NULL UNIQUE
                              CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    -- Nullable: social-only accounts (e.g. Google) have no password.
    password_hash TEXT        NULL
                              CHECK (password_hash IS NULL OR char_length(password_hash) >= 60),
    -- Google account subject ('sub' claim); NULL for password-only accounts.
    google_sub    TEXT        NULL UNIQUE,
    email_verified BOOLEAN     NOT NULL DEFAULT false,
    role          user_role   NOT NULL DEFAULT 'user',
    last_login_at TIMESTAMPTZ NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Every account must have at least one way to authenticate.
    CONSTRAINT users_auth_method_present CHECK (password_hash IS NOT NULL OR google_sub IS NOT NULL)
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
-- State Address Register (VZD) mirror
--
-- A weekly-refreshed copy of Latvia's official address register (open data,
-- CC-BY-4.0) powering strict street/house autocomplete. Rows are wiped and
-- reloaded by AddressRegistryIngestService, so user data never references
-- these tables with foreign keys — the stable VZD codes are the linkage.
-- ─────────────────────────────────────────────
CREATE TABLE address_territories (
    code             BIGINT   PRIMARY KEY,  -- VZD KODS
    type_cd          SMALLINT NOT NULL,     -- 104 city, 105 parish, 106 village
    name             TEXT     NOT NULL,
    norm_name        TEXT     NOT NULL,     -- diacritics-folded lowercase, for matching
    novads_name      TEXT,                  -- resolved municipality; NULL for republic cities
    norm_novads_name TEXT
);

CREATE INDEX idx_address_territories_norm_name   ON address_territories (norm_name);
CREATE INDEX idx_address_territories_norm_novads ON address_territories (norm_novads_name);

CREATE TABLE address_streets (
    code           BIGINT PRIMARY KEY,      -- VZD KODS
    territory_code BIGINT NOT NULL REFERENCES address_territories (code) ON DELETE CASCADE,
    name           TEXT   NOT NULL,
    norm_name      TEXT   NOT NULL
);

CREATE INDEX idx_address_streets_territory_norm ON address_streets (territory_code, norm_name text_pattern_ops);

CREATE TABLE address_buildings (
    code           BIGINT PRIMARY KEY,      -- VZD KODS
    street_code    BIGINT REFERENCES address_streets (code) ON DELETE CASCADE,  -- NULL: rural house named directly under its territory
    territory_code BIGINT NOT NULL REFERENCES address_territories (code) ON DELETE CASCADE,
    name           TEXT   NOT NULL,         -- house number ("12", "12 k-1") or rural house name
    norm_name      TEXT   NOT NULL,
    lat            DOUBLE PRECISION,
    lng            DOUBLE PRECISION
);

CREATE INDEX idx_address_buildings_street_norm    ON address_buildings (street_code, norm_name text_pattern_ops);
CREATE INDEX idx_address_buildings_territory_norm ON address_buildings (territory_code, norm_name text_pattern_ops)
    WHERE street_code IS NULL;

-- Individual apartments (VZD "dzīvoklis"), each with their own VAR address
-- code. Links a listing's free-typed apartment number to the official code
-- the cadastre mirror's premise groups reference (see cadastre_premises).
CREATE TABLE address_apartments (
    code           BIGINT PRIMARY KEY,      -- VZD KODS
    building_code  BIGINT NOT NULL REFERENCES address_buildings (code) ON DELETE CASCADE,
    name           TEXT   NOT NULL,         -- apartment number as VZD names it ("10", "3A")
    norm_name      TEXT   NOT NULL
);

CREATE INDEX idx_address_apartments_building_norm ON address_apartments (building_code, norm_name);

-- ─────────────────────────────────────────────
-- VZD Cadastre mirror
--
-- A weekly-refreshed copy of Latvia's official real-property cadastre (open
-- data, CC-BY-4.0), cross-checked against a listing's posted area/build year
-- at creation and edit time. No FKs to the address_* mirror: both are wiped
-- and reloaded independently by their own ingest services.
-- ─────────────────────────────────────────────
CREATE TABLE cadastre_buildings (
    cadastre_nr      TEXT PRIMARY KEY,   -- VZD BuildingCadastreNr
    ar_building_code BIGINT,             -- VZD VARISCode; matches address_buildings.code
    year_built       SMALLINT
);

CREATE INDEX idx_cadastre_buildings_ar_code ON cadastre_buildings (ar_building_code);

CREATE TABLE cadastre_premises (
    cadastre_nr TEXT PRIMARY KEY,        -- VZD PremiseGroupCadastreNr
    ar_code     BIGINT,                  -- VZD PremiseGroupVARISCode; matches address_apartments.code
    area_m2     NUMERIC(8, 2)
);

CREATE INDEX idx_cadastre_premises_ar_code ON cadastre_premises (ar_code);

-- Land parcels: the cadastral source for land / commercial listings. Carries the
-- parcel's official area and land-use purpose, matched to a property by cadastre_nr.
CREATE TABLE cadastre_parcels (
    cadastre_nr TEXT PRIMARY KEY,        -- VZD ParcelCadastreNr
    area_m2     NUMERIC(12, 2),
    land_use    land_use                 -- mapped from VZD land-use purpose; NULL if unpublished
);

CREATE INDEX idx_cadastre_parcels_land_use ON cadastre_parcels (land_use);

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
    -- Strict-address linkage: the State Address Register building code the
    -- address was picked from, plus the free-typed apartment number. NULL on
    -- rows created before the register integration (legacy free-text addresses).
    -- No FK: address_buildings is wiped and reloaded on every register refresh.
    ar_building_code  BIGINT,
    apartment         TEXT,
    -- Cadastral parcel the address/plot was picked from (land & commercial). No FK:
    -- cadastre_parcels is wiped and reloaded on every cadastre refresh.
    cadastre_parcel_nr TEXT,
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
    -- Sub-type axis: required per category (see CHECKs below). new_project reuses the
    -- property_category enum for its apartment|house kind; commercial has its own enum.
    new_project_kind    property_category,
    commercial_subtype  commercial_type,
    -- Land-use purpose for land / commercial; cadastre-sourced (auto-populated / validated).
    land_use            land_use,

    -- Physical dimensions. rooms/m2 are nullable: land & garage have no rooms, land
    -- has no building area. Presence is gated per-category by CHECKs below + validator.
    rooms               SMALLINT          CHECK (rooms > 0),
    bedrooms            SMALLINT          CHECK (bedrooms >= 0 AND bedrooms <= rooms),
    bathrooms           SMALLINT          CHECK (bathrooms >= 0),
    bathroom_layout     bathroom_layout,
    m2                  NUMERIC(6, 2)     CHECK (m2 > 0),
    -- Plot area: houses (garden) and land (parcel size). land_m2 reused for both.
    land_m2             NUMERIC(8, 2)     CHECK (land_m2 > 0),
    floor               SMALLINT          CHECK (floor >= 0),
    total_floors        SMALLINT          CHECK (total_floors > 0),
    year_built          SMALLINT          CHECK (year_built BETWEEN 1800 AND 2200),

    -- Building characteristics
    heating             heating_type,
    energy_class        energy_class,
    maintenance_cost    NUMERIC(10, 2)    CHECK (maintenance_cost >= 0),   -- monthly, EUR
    sewage              sewage_type,
    ventilation         ventilation_type,
    roof                roof_type,

    -- Media: ordered arrays of URL strings; array order is display order
    photos              JSONB             NOT NULL DEFAULT '[]',
    plans               JSONB             NOT NULL DEFAULT '[]',
    video_url           TEXT,
    website_url         TEXT,

    -- new_project specific; 'not_ready → no year_built' rule enforced at service layer
    completion          property_completion,

    status              property_status   NOT NULL DEFAULT 'active',
    posted_at           TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ       NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ       NOT NULL,
    expiry_warning_sent BOOLEAN           NOT NULL DEFAULT false,

    -- Contact phones: ordered array of {phone, name, email} objects; array order
    -- is display order. name/email default to the owner's account values when
    -- left blank on write (see PropertyMapper#applyListingContent).
    phones              JSONB             NOT NULL DEFAULT '[]',

    -- Plot area only on houses and land
    CONSTRAINT chk_land_m2_scope
        CHECK (property_category IN ('house', 'land') OR land_m2 IS NULL),
    CONSTRAINT chk_floor_requires_total
        CHECK (floor IS NULL OR total_floors IS NOT NULL),
    CONSTRAINT chk_floor_lte_total
        CHECK (floor IS NULL OR floor <= total_floors),
    -- completion is a new_project-only attribute
    CONSTRAINT chk_completion_new_project_only
        CHECK (property_category = 'new_project' OR completion IS NULL),
    -- Sub-type presence is category-bound
    CONSTRAINT chk_new_project_kind
        CHECK ((property_category = 'new_project') = (new_project_kind IS NOT NULL)
               AND (new_project_kind IS NULL OR new_project_kind IN ('apartment', 'house'))),
    CONSTRAINT chk_commercial_subtype
        CHECK ((property_category = 'commercial') = (commercial_subtype IS NOT NULL)),
    CONSTRAINT chk_land_use_scope
        CHECK (land_use IS NULL OR property_category IN ('land', 'commercial')),
    -- rooms only apply to dwellings and commercial; land & garage have none
    CONSTRAINT chk_rooms_scope
        CHECK (property_category IN ('apartment', 'house', 'new_project', 'commercial')
               OR rooms IS NULL)
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
-- Listing attribute sets (multi-select property attributes)
-- ─────────────────────────────────────────────
CREATE TABLE listing_ventilation_systems (
    listing_id         UUID               NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    ventilation_system ventilation_system NOT NULL,
    PRIMARY KEY (listing_id, ventilation_system)
);
CREATE INDEX idx_listing_ventilation_systems ON listing_ventilation_systems (ventilation_system);

CREATE TABLE listing_communications (
    listing_id    UUID               NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    communication communication_type NOT NULL,
    PRIMARY KEY (listing_id, communication)
);
CREATE INDEX idx_listing_communications ON listing_communications (communication);

CREATE TABLE listing_stove (
    listing_id UUID       NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    stove      stove_type NOT NULL,
    PRIMARY KEY (listing_id, stove)
);
CREATE INDEX idx_listing_stove ON listing_stove (stove);

CREATE TABLE listing_security (
    listing_id UUID             NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    security   security_feature NOT NULL,
    PRIMARY KEY (listing_id, security)
);
CREATE INDEX idx_listing_security ON listing_security (security);

CREATE TABLE listing_extras (
    listing_id UUID           NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    extra      property_extra NOT NULL,
    PRIMARY KEY (listing_id, extra)
);
CREATE INDEX idx_listing_extras ON listing_extras (extra);

CREATE TABLE listing_parking (
    listing_id UUID         NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    parking    parking_type NOT NULL,
    PRIMARY KEY (listing_id, parking)
);
CREATE INDEX idx_listing_parking ON listing_parking (parking);

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
