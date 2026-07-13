-- Migration: minimal admin role + listing moderation (run once against an
-- existing database; db/schema.sql already contains the final shape for
-- fresh installs).
--
-- 1. A user_role enum + column, defaulting every existing user to 'user'.
--    Promote an admin by hand: UPDATE users SET role = 'admin' WHERE email = '...';
-- 2. A new 'pending_review' listing status, held for listings whose posted
--    m2/year_built disagree with the VZD cadastre register (see
--    migrate_cadastre_register.sql). ALTER TYPE ... ADD VALUE must run outside
--    any transaction that references the new value, so this migration must be
--    applied (and committed) before any code path can use 'pending_review'.

CREATE TYPE user_role AS ENUM ('user', 'admin');

ALTER TABLE users
    ADD COLUMN role user_role NOT NULL DEFAULT 'user';

ALTER TYPE property_status ADD VALUE 'pending_review';
