-- Baltnami — development seed data
-- 5 users · 13 addresses · 14 self-contained listings (one address has a
-- sell + rent-a-floor pair) · photos/plans/phones as JSONB · listing
-- translations · listing features · saved listings
-- All passwords = 'Secret123!'  (bcrypt cost 12, placeholder hashes)

BEGIN;

-- ─────────────────────────────────────────────
-- Users
-- ─────────────────────────────────────────────
INSERT INTO users (id, name, email, password_hash, email_verified) VALUES
    ('10000000-0000-0000-0000-000000000001',
     'Jānis Bērziņš',   'janis.berzins@gmail.com',
     '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.s5uJNi', true),
    ('10000000-0000-0000-0000-000000000002',
     'Anna Kalniņa',    'anna.kalnina@inbox.lv',
     '$2b$12$eImiTXuWVxfM37uY4JANjOe5XdmkxMBvbMoAhvAKMZniTOVKAi0hi', true),
     ('10000000-0000-0000-0000-000000000003',
     'Māris Ozoliņš',   'maris.ozolins@gmail.com',
     '$2b$12$WApznUOJfkEGSmYRfnkrPOr6fKlbwnYrQnFPJNuGGcR7qOJnuF/Aq', true),
    ('10000000-0000-0000-0000-000000000004',
     'Laura Liepiņa',   'laura.liepina@draugiem.lv',
     '$2b$12$VfZnO3FkxAUe2m5PxuIq5u0A3vU5NpG2XiT1iIRhFbJhCq3w3YSmy', true),
    ('10000000-0000-0000-0000-000000000005',
     'Pēteris Krūmiņš', 'peteris.krumins@gmail.com',
     '$2b$12$92In5/RuASAH5A3HNMCIGeDXPfSfmJBWZgNgK2aWdnVPqSJ4G5CpO', true);

-- ─────────────────────────────────────────────
-- Properties (shared address registry)
-- ─────────────────────────────────────────────
INSERT INTO properties (id, owner_id, district_slug, city_slug, address, lat, lng) VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'centrs',        'riga',    'Dzirnavu iela 57-18',    56.9490, 24.1052),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'teika',         'riga',    'Hospitāļu iela 12-34',   56.9655, 24.1480),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'majori',        'jurmala', 'Jomas iela 88',          56.9710, 23.7703),
    ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', 'purvciems',     'riga',    'Stirnu iela 22-81',      56.9743, 24.1890),
    ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000002', 'agenskalns',    'riga',    'Nometņu iela 74-3',      56.9375, 24.0714),
    ('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000003', 'imanta',        'riga',    'Eduarda Veidenbauma iela 5', 56.9560, 24.0000),
    ('20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000003', 'ziepniekkalns', 'riga',    'Zeļļu iela 3-12',        56.9095, 24.0690),
    ('20000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000003', 'mezciems',      'riga',    'Vīlipa iela 8-64',       56.9820, 24.1680),
    ('20000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000004', 'centrs',        'riga',    'Elizabetes iela 31-5A',  56.9530, 24.1120),
    ('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000004', 'teika',         'riga',    'Brīvības gatve 204-17',  56.9680, 24.1550),
    ('20000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000005', 'jugla',         'riga',    'Juglas iela 14',         56.9930, 24.2420),
    ('20000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000005', 'centrs',        'riga',    'Čaka iela 42-7',         56.9511, 24.1218),
    ('20000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000005', 'purvciems',     'riga',    'Dzelzavas iela 60-52',   56.9760, 24.1920);

-- ─────────────────────────────────────────────
-- Listings (self-contained market offers)
-- ─────────────────────────────────────────────
INSERT INTO listings (
    id, property_id, owner_id,
    listing_type, price, completion,
    property_category,
    rooms, bedrooms, bathrooms, bathroom_layout, m2, land_m2, floor, total_floors, year_built,
    heating, energy_class, maintenance_cost,
    photos, plans, video_url,
    posted_at, expires_at,
    phones
) VALUES

('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
 'buy', 185000.00, NULL,
 'apartment', 2, 1, 1, 'combined', 52.40, NULL, 4, 9, 1968, 'central', 'D', 45.00,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000001/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000001/1.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000001/2.jpg"]'::jsonb,
 '[]'::jsonb, 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
 '2026-06-01 09:15:00+03', '2026-06-01 09:15:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 29 123 456", "name": "Jānis Bērziņš", "email": "janis.berzins@gmail.com"}, {"phone": "+371 26 789 012", "name": "Jānis Bērziņš", "email": "janis.berzins@gmail.com"}]'::jsonb),

('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001',
 'rent', 650.00, NULL,
 'apartment', 3, 2, 1, 'separate', 68.00, NULL, 2, 5, 1985, 'central', 'E', 60.00,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000002/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000002/1.jpg"]'::jsonb,
 '[]'::jsonb, NULL,
 '2026-05-20 14:30:00+03', '2026-05-20 14:30:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 29 234 567", "name": "Jānis Bērziņš", "email": "janis.berzins@gmail.com"}]'::jsonb),

('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001',
 'buy', 320000.00, NULL,
 'house', 4, 3, 2, 'separate', 120.00, 400.00, NULL, NULL, 2005, 'gas', 'C', NULL,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000003/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000003/1.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000003/2.jpg"]'::jsonb,
 '[]'::jsonb, 'https://www.youtube.com/watch?v=xvFZjo5PgG0',
 '2026-04-11 10:00:00+03', '2026-04-11 10:00:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 26 345 678", "name": "Jānis Bērziņš", "email": "janis.berzins@gmail.com"}, {"phone": "+371 29 456 789", "name": "Jānis Bērziņš", "email": "janis.berzins@gmail.com"}]'::jsonb),

('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002',
 'buy', 75000.00, NULL,
 'apartment', 1, 0, 1, 'combined', 38.20, NULL, 8, 12, 2002, 'electric', 'D', 35.00,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000004/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000004/1.jpg"]'::jsonb,
 '[]'::jsonb, NULL,
 '2026-06-05 11:00:00+03', '2026-06-05 11:00:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 22 567 890", "name": "Anna Kalniņa", "email": "anna.kalnina@inbox.lv"}]'::jsonb),

('30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000002',
 'new_project', 145000.00, 'not_ready',
 'apartment', 2, 1, 1, 'combined', 58.00, NULL, 3, 8, NULL, 'heat_pump', 'A', 55.00,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000005/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000005/1.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000005/2.jpg"]'::jsonb,
 '[]'::jsonb, 'https://www.youtube.com/watch?v=L_jWHffIx5E',
 '2026-05-30 16:45:00+03', '2026-05-30 16:45:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 29 678 901", "name": "Anna Kalniņa", "email": "anna.kalnina@inbox.lv"}]'::jsonb),

('30000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000003',
 'buy', 295000.00, NULL,
 'house', 5, 4, 2, 'separate', 180.00, 650.00, NULL, NULL, 1998, 'gas', 'D', NULL,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000006/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000006/1.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000006/2.jpg"]'::jsonb,
 '[]'::jsonb, NULL,
 '2026-03-28 09:00:00+03', '2026-03-28 09:00:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 26 111 222", "name": "Māris Ozoliņš", "email": "maris.ozolins@gmail.com"}, {"phone": "+371 29 333 444", "name": "Māris Ozoliņš", "email": "maris.ozolins@gmail.com"}]'::jsonb),

('30000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000003',
 'rent', 480.00, NULL,
 'apartment', 2, 1, 1, 'combined', 54.00, NULL, 0, 9, 1980, 'central', 'F', 40.00,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000007/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000007/1.jpg"]'::jsonb,
 '[]'::jsonb, NULL,
 '2026-06-08 13:20:00+03', '2026-06-08 13:20:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 22 555 666", "name": "Māris Ozoliņš", "email": "maris.ozolins@gmail.com"}]'::jsonb),

('30000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000003',
 'buy', 95000.00, NULL,
 'apartment', 3, 2, 1, 'separate', 72.00, NULL, 6, 9, 1985, 'central', 'E', 50.00,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000008/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000008/1.jpg"]'::jsonb,
 '[]'::jsonb, NULL,
 '2026-04-22 10:30:00+03', '2026-04-22 10:30:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 29 777 888", "name": "Māris Ozoliņš", "email": "maris.ozolins@gmail.com"}]'::jsonb),

('30000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000004',
 'new_project', 220000.00, 'ready',
 'apartment', 2, 1, 1, 'separate', 62.00, NULL, 5, 12, 2024, 'heat_pump', 'A', 65.00,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000009/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000009/1.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000009/2.jpg"]'::jsonb,
 '[]'::jsonb, 'https://www.youtube.com/watch?v=J---aiyznGQ',
 '2026-06-10 08:00:00+03', '2026-06-10 08:00:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 26 999 000", "name": "Laura Liepiņa", "email": "laura.liepina@draugiem.lv"}, {"phone": "+371 29 112 233", "name": "Laura Liepiņa", "email": "laura.liepina@draugiem.lv"}]'::jsonb),

('30000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000004',
 'rent', 420.00, NULL,
 'apartment', 1, 0, 1, 'combined', 32.00, NULL, 3, 5, 1974, 'central', 'E', 30.00,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000010/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000010/1.jpg"]'::jsonb,
 '[]'::jsonb, NULL,
 '2026-05-14 17:00:00+03', '2026-05-14 17:00:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 22 445 566", "name": "Laura Liepiņa", "email": "laura.liepina@draugiem.lv"}]'::jsonb),

('30000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000005',
 'buy', 245000.00, NULL,
 'house', 4, 3, 2, 'separate', 145.00, 520.00, NULL, NULL, 2012, 'heat_pump', 'B', NULL,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000011/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000011/1.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000011/2.jpg"]'::jsonb,
 '[]'::jsonb, NULL,
 '2026-05-05 12:00:00+03', '2026-05-05 12:00:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 29 667 788", "name": "Pēteris Krūmiņš", "email": "peteris.krumins@gmail.com"}]'::jsonb),

('30000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000005',
 'buy', 155000.00, NULL,
 'apartment', 2, 1, 1, 'combined', 65.00, NULL, 3, 6, 1955, 'central', 'F', 48.00,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000012/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000012/1.jpg"]'::jsonb,
 '[]'::jsonb, NULL,
 '2026-06-03 15:10:00+03', '2026-06-03 15:10:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 26 889 900", "name": "Pēteris Krūmiņš", "email": "peteris.krumins@gmail.com"}]'::jsonb),

('30000000-0000-0000-0000-000000000013', '20000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000005',
 'rent', 750.00, NULL,
 'apartment', 3, 2, 1, 'separate', 75.00, NULL, 5, 9, 1990, 'central', 'D', 55.00,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000013/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000013/1.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000013/2.jpg"]'::jsonb,
 '[]'::jsonb, NULL,
 '2026-05-25 09:45:00+03', '2026-05-25 09:45:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 29 223 344", "name": "Pēteris Krūmiņš", "email": "peteris.krumins@gmail.com"}, {"phone": "+371 22 556 677", "name": "Pēteris Krūmiņš", "email": "peteris.krumins@gmail.com"}]'::jsonb),

-- ── Second listing at the Jūrmala house (20…003): the owner also rents out its
--    ground floor. Same address, its own scope (one floor) and a trimmed photo set.
('30000000-0000-0000-0000-000000000014', '20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001',
 'rent', 900.00, NULL,
 'apartment', 2, 1, 1, 'combined', 60.00, NULL, 1, 2, 2005, 'gas', 'C', NULL,
 '["https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000003/0.jpg",
   "https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000003/2.jpg"]'::jsonb,
 '[]'::jsonb, NULL,
 '2026-06-12 10:00:00+03', '2026-06-12 10:00:00+03'::timestamptz + interval '90 days',
 '[{"phone": "+371 26 345 678", "name": "Jānis Bērziņš", "email": "janis.berzins@gmail.com"}]'::jsonb);

-- ─────────────────────────────────────────────
-- Listing features
-- ─────────────────────────────────────────────
INSERT INTO listing_features (listing_id, feature) VALUES
    ('30000000-0000-0000-0000-000000000001', 'balcony'),
    ('30000000-0000-0000-0000-000000000001', 'elevator'),
    ('30000000-0000-0000-0000-000000000002', 'furnished'),
    ('30000000-0000-0000-0000-000000000002', 'pets'),
    ('30000000-0000-0000-0000-000000000003', 'parking'),
    ('30000000-0000-0000-0000-000000000004', 'elevator'),
    ('30000000-0000-0000-0000-000000000004', 'balcony'),
    ('30000000-0000-0000-0000-000000000005', 'new_building'),
    ('30000000-0000-0000-0000-000000000005', 'elevator'),
    ('30000000-0000-0000-0000-000000000005', 'balcony'),
    ('30000000-0000-0000-0000-000000000005', 'parking'),
    ('30000000-0000-0000-0000-000000000006', 'parking'),
    ('30000000-0000-0000-0000-000000000006', 'balcony'),
    ('30000000-0000-0000-0000-000000000007', 'pets'),
    ('30000000-0000-0000-0000-000000000008', 'balcony'),
    ('30000000-0000-0000-0000-000000000009', 'new_building'),
    ('30000000-0000-0000-0000-000000000009', 'elevator'),
    ('30000000-0000-0000-0000-000000000009', 'balcony'),
    ('30000000-0000-0000-0000-000000000009', 'parking'),
    ('30000000-0000-0000-0000-000000000010', 'furnished'),
    ('30000000-0000-0000-0000-000000000011', 'parking'),
    ('30000000-0000-0000-0000-000000000011', 'balcony'),
    ('30000000-0000-0000-0000-000000000012', 'balcony'),
    ('30000000-0000-0000-0000-000000000012', 'elevator'),
    ('30000000-0000-0000-0000-000000000013', 'furnished'),
    ('30000000-0000-0000-0000-000000000013', 'balcony'),
    -- l14: rented ground floor of the Jūrmala house
    ('30000000-0000-0000-0000-000000000014', 'parking'),
    ('30000000-0000-0000-0000-000000000014', 'furnished');

-- ─────────────────────────────────────────────
-- Listing translations
-- ─────────────────────────────────────────────
INSERT INTO listing_translations (listing_id, locale, title, description) VALUES
    -- l01: centrs apartment
    ('30000000-0000-0000-0000-000000000001', 'lv', 'Gaišs 2-istabu dzīvoklis Centrā', 'Kluss pagalms, augsti griesti, svaigs remonts. Blakus Vērmanes dārzs.'),
    ('30000000-0000-0000-0000-000000000001', 'en', 'Bright 2-room apartment in Centrs', 'Quiet courtyard, high ceilings, fresh renovation. Near Vērmanes Garden.'),
    -- l02: teika rental
    ('30000000-0000-0000-0000-000000000002', 'lv', '3-istabu Teikā, mēbeles iekļautas', 'Pilnībā mēbelēts dzīvoklis, mājdzīvnieki atļauti.'),
    ('30000000-0000-0000-0000-000000000002', 'en', '3-room in Teika, furniture included', 'Fully furnished apartment, pets allowed.'),
    -- l03: jurmala house
    ('30000000-0000-0000-0000-000000000003', 'lv', 'Māja Jūrmalā ar dārzu 400 m²', 'Ķieģeļu māja 2005. g., silta garāža, 10 min līdz pludmalei.'),
    ('30000000-0000-0000-0000-000000000003', 'en', 'House in Jūrmala with 400 m² garden', 'Brick house built 2005, heated garage, 10 min to the beach.'),
    -- l04: purvciems studio
    ('30000000-0000-0000-0000-000000000004', 'lv', 'Studija Purvciemā — pirmais pirkums', 'Lielisks variants investīcijai vai pirmajam mājoklim. Lifts.'),
    ('30000000-0000-0000-0000-000000000004', 'en', 'Studio in Purvciems — first-time buy', 'Great option for investment or first home. Elevator.'),
    -- l05: agenskalns new_project
    ('30000000-0000-0000-0000-000000000005', 'lv', 'Jaunbūve Āgenskalnā — nodošana 2027', 'Panorāmas logi, slēgts pagalms, autostāvvieta iekļauta.'),
    ('30000000-0000-0000-0000-000000000005', 'en', 'New build in Āgenskalns — completion 2027', 'Panoramic windows, enclosed courtyard, parking spot included.'),
    -- l06: imanta house
    ('30000000-0000-0000-0000-000000000006', 'lv', 'Privātmāja Imantā ar lielu zemes gabalu', 'Divstāvu māja, 650 m² zeme, garāža 2 mašīnām, pirts.'),
    ('30000000-0000-0000-0000-000000000006', 'en', 'Detached house in Imanta with large plot', 'Two-storey house, 650 m² land, double garage, sauna.'),
    -- l07: ziepniekkalns rental
    ('30000000-0000-0000-0000-000000000007', 'lv', '2-istabu īre pirmajā stāvā', 'Pirmais stāvs, kaķi/suņi līdz 10 kg. Bez mēbelēm.'),
    ('30000000-0000-0000-0000-000000000007', 'en', '2-room rental on ground floor', 'Ground floor, cats/dogs up to 10 kg. Unfurnished.'),
    -- l08: mezciems apartment (LV only)
    ('30000000-0000-0000-0000-000000000008', 'lv', '3-istabu Mežciemā — labs stāvoklis', 'Balkons, noliktava, kluss pagalms. Padomju laika ēka, kapitālremonts 2018.'),
    -- l09: centrs new_project ready
    ('30000000-0000-0000-0000-000000000009', 'lv', 'Gatava jaunbūve Centrā — atslēgas uzreiz', 'A klase, viedā māja, pazemes autostāvvieta, konsjeržs.'),
    ('30000000-0000-0000-0000-000000000009', 'en', 'Ready new build in Centrs — keys now', 'Class A, smart home, underground parking, concierge.'),
    -- l10: teika studio
    ('30000000-0000-0000-0000-000000000010', 'lv', 'Mājīga studija Teikā', 'Pēc remonta, iebūvēta virtuve, blakus tramvajs.'),
    ('30000000-0000-0000-0000-000000000010', 'en', 'Cozy studio in Teika', 'Renovated, built-in kitchen, tram nearby.'),
    -- l11: jugla house
    ('30000000-0000-0000-0000-000000000011', 'lv', 'Moderna māja Juglā, 2012. g.', 'Monolīta, siltināta, garāža, kopts dārzs.'),
    ('30000000-0000-0000-0000-000000000011', 'en', 'Modern house in Jugla, built 2012', 'Monolithic, insulated, garage, well-kept garden.'),
    -- l12: centrs stalinka (EN only)
    ('30000000-0000-0000-0000-000000000012', 'en', '2-room in Centrs — Stalinist building', 'High ceilings 3.1 m, parquet, stucco. Needs cosmetic work.'),
    -- l13: purvciems rental
    ('30000000-0000-0000-0000-000000000013', 'lv', '3-istabu Purvciemā ar mēbelēm', 'Pilnībā mēbelēts, balkons, kluss pagalms.'),
    ('30000000-0000-0000-0000-000000000013', 'en', '3-room in Purvciems with furniture', 'Fully furnished, balcony, quiet courtyard.'),
    -- l14: rented ground floor of the Jūrmala house (same address as l03)
    ('30000000-0000-0000-0000-000000000014', 'lv', 'Jūrmalas mājas 1. stāvs īrei', 'Atsevišķa ieeja, 2 istabas, sava terase. Tā pati māja, kas pārdošanā.'),
    ('30000000-0000-0000-0000-000000000014', 'en', 'Ground floor of the Jūrmala house for rent', 'Separate entrance, 2 rooms, own terrace. The same house that is also for sale.');

-- ─────────────────────────────────────────────
-- Saved listings
-- ─────────────────────────────────────────────
INSERT INTO saved_listings (user_id, listing_id) VALUES
    -- u1 (Jānis) saved the new_project and the Jugla house
    ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000009'),
    ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000011'),
    -- u2 (Anna) saved the centrs apartment
    ('10000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001'),
    -- u3 (Māris) saved the centrs new_project and the centrs stalinka
    ('10000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000009'),
    ('10000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001'),
    -- u4 (Laura) saved the Imanta house
    ('10000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000006'),
    -- u5 (Pēteris) saved the purvciems studio
    ('10000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000004');

COMMIT;
