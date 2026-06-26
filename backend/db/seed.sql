-- Baltnami — development seed data
-- 5 users · 13 properties · photos (S3 URLs) · saved listings
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
-- Properties
-- ─────────────────────────────────────────────
INSERT INTO properties (
    id, owner_id,
    listing_type, property_category,
    price,
    rooms, m2, land_m2, floor, total_floors, year_built,
    completion, video_url,
    district_slug, city_slug, address, lat, lng,
    posted_at
) VALUES

-- ── Jānis Bērziņš (u1) ──────────────────────────────────────────────────────

('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
 'buy', 'apartment', 185000.00,
 2, 52.40, NULL, 4, 9, 1968,
 NULL, 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
 'centrs', 'riga', 'Dzirnavu iela 57-18', 56.9490, 24.1052,
 '2026-06-01 09:15:00+03'),

('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001',
 'rent', 'apartment', 650.00,
 3, 68.00, NULL, 2, 5, 1985,
 NULL, NULL,
 'teika', 'riga', 'Hospitāļu iela 12-34', 56.9655, 24.1480,
 '2026-05-20 14:30:00+03'),

('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001',
 'buy', 'house', 320000.00,
 4, 120.00, 400.00, NULL, NULL, 2005,
 NULL, 'https://www.youtube.com/watch?v=xvFZjo5PgG0',
 'majori', 'jurmala', 'Jomas iela 88', 56.9710, 23.7703,
 '2026-04-11 10:00:00+03'),

-- ── Anna Kalniņa (u2) ───────────────────────────────────────────────────────

('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002',
 'buy', 'apartment', 75000.00,
 1, 38.20, NULL, 8, 12, 2002,
 NULL, NULL,
 'purvciems', 'riga', 'Stirnu iela 22-81', 56.9743, 24.1890,
 '2026-06-05 11:00:00+03'),

('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000002',
 'new_project', 'apartment', 145000.00,
 2, 58.00, NULL, 3, 8, NULL,
 'not_ready', 'https://www.youtube.com/watch?v=L_jWHffIx5E',
 'agenskalns', 'riga', 'Nometņu iela 74-3', 56.9375, 24.0714,
 '2026-05-30 16:45:00+03'),

-- ── Māris Ozoliņš (u3) ──────────────────────────────────────────────────────

('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000003',
 'buy', 'house', 295000.00,
 5, 180.00, 650.00, NULL, NULL, 1998,
 NULL, NULL,
 'imanta', 'riga', 'Eduarda Veidenbauma iela 5', 56.9560, 24.0000,
 '2026-03-28 09:00:00+03'),

('20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000003',
 'rent', 'apartment', 480.00,
 2, 54.00, NULL, 0, 9, 1980,
 NULL, NULL,
 'ziepniekkalns', 'riga', 'Zeļļu iela 3-12', 56.9095, 24.0690,
 '2026-06-08 13:20:00+03'),

('20000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000003',
 'buy', 'apartment', 95000.00,
 3, 72.00, NULL, 6, 9, 1985,
 NULL, NULL,
 'mezciems', 'riga', 'Vīlipa iela 8-64', 56.9820, 24.1680,
 '2026-04-22 10:30:00+03'),

-- ── Laura Liepiņa (u4) ──────────────────────────────────────────────────────

('20000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000004',
 'new_project', 'apartment', 220000.00,
 2, 62.00, NULL, 5, 12, 2024,
 'ready', 'https://www.youtube.com/watch?v=J---aiyznGQ',
 'centrs', 'riga', 'Elizabetes iela 31-5A', 56.9530, 24.1120,
 '2026-06-10 08:00:00+03'),

('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000004',
 'rent', 'apartment', 420.00,
 1, 32.00, NULL, 3, 5, 1974,
 NULL, NULL,
 'teika', 'riga', 'Brīvības gatve 204-17', 56.9680, 24.1550,
 '2026-05-14 17:00:00+03'),

-- ── Pēteris Krūmiņš (u5) ────────────────────────────────────────────────────

('20000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000005',
 'buy', 'house', 245000.00,
 4, 145.00, 520.00, NULL, NULL, 2012,
 NULL, NULL,
 'jugla', 'riga', 'Juglas iela 14', 56.9930, 24.2420,
 '2026-05-05 12:00:00+03'),

('20000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000005',
 'buy', 'apartment', 155000.00,
 2, 65.00, NULL, 3, 6, 1955,
 NULL, NULL,
 'centrs', 'riga', 'Čaka iela 42-7', 56.9511, 24.1218,
 '2026-06-03 15:10:00+03'),

('20000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000005',
 'rent', 'apartment', 750.00,
 3, 75.00, NULL, 5, 9, 1990,
 NULL, NULL,
 'purvciems', 'riga', 'Dzelzavas iela 60-52', 56.9760, 24.1920,
 '2026-05-25 09:45:00+03');

-- ─────────────────────────────────────────────
-- Translations
-- ─────────────────────────────────────────────
INSERT INTO property_translations (property_id, locale, title, description) VALUES
    -- p01: centrs apartment
    ('20000000-0000-0000-0000-000000000001', 'lv', 'Gaišs 2-istabu dzīvoklis Centrā', 'Kluss pagalms, augsti griesti, svaigs remonts. Blakus Vērmanes dārzs.'),
    ('20000000-0000-0000-0000-000000000001', 'en', 'Bright 2-room apartment in Centrs', 'Quiet courtyard, high ceilings, fresh renovation. Near Vērmanes Garden.'),
    -- p02: teika rental
    ('20000000-0000-0000-0000-000000000002', 'lv', '3-istabu Teikā, mēbeles iekļautas', 'Pilnībā mēbelēts dzīvoklis, mājdzīvnieki atļauti.'),
    ('20000000-0000-0000-0000-000000000002', 'en', '3-room in Teika, furniture included', 'Fully furnished apartment, pets allowed.'),
    -- p03: jurmala house
    ('20000000-0000-0000-0000-000000000003', 'lv', 'Māja Jūrmalā ar dārzu 400 m²', 'Ķieģeļu māja 2005. g., silta garāža, 10 min līdz pludmalei.'),
    ('20000000-0000-0000-0000-000000000003', 'en', 'House in Jūrmala with 400 m² garden', 'Brick house built 2005, heated garage, 10 min to the beach.'),
    -- p04: purvciems studio
    ('20000000-0000-0000-0000-000000000004', 'lv', 'Studija Purvciemā — pirmais pirkums', 'Lielisks variants investīcijai vai pirmajam mājoklim. Lifts.'),
    ('20000000-0000-0000-0000-000000000004', 'en', 'Studio in Purvciems — first-time buy', 'Great option for investment or first home. Elevator.'),
    -- p05: agenskalns new_project
    ('20000000-0000-0000-0000-000000000005', 'lv', 'Jaunbūve Āgenskalnā — nodošana 2027', 'Panorāmas logi, slēgts pagalms, autostāvvieta iekļauta.'),
    ('20000000-0000-0000-0000-000000000005', 'en', 'New build in Āgenskalns — completion 2027', 'Panoramic windows, enclosed courtyard, parking spot included.'),
    -- p06: imanta house
    ('20000000-0000-0000-0000-000000000006', 'lv', 'Privātmāja Imantā ar lielu zemes gabalu', 'Divstāvu māja, 650 m² zeme, garāža 2 mašīnām, pirts.'),
    ('20000000-0000-0000-0000-000000000006', 'en', 'Detached house in Imanta with large plot', 'Two-storey house, 650 m² land, double garage, sauna.'),
    -- p07: ziepniekkalns rental
    ('20000000-0000-0000-0000-000000000007', 'lv', '2-istabu īre pirmajā stāvā', 'Pirmais stāvs, kaķi/suņi līdz 10 kg. Bez mēbelēm.'),
    ('20000000-0000-0000-0000-000000000007', 'en', '2-room rental on ground floor', 'Ground floor, cats/dogs up to 10 kg. Unfurnished.'),
    -- p08: mezciems apartment (LV only)
    ('20000000-0000-0000-0000-000000000008', 'lv', '3-istabu Mežciemā — labs stāvoklis', 'Balkons, noliktava, kluss pagalms. Padomju laika ēka, kapitālremonts 2018.'),
    -- p09: centrs new_project ready
    ('20000000-0000-0000-0000-000000000009', 'lv', 'Gatava jaunbūve Centrā — atslēgas uzreiz', 'A klase, viedā māja, pazemes autostāvvieta, konsjeržs.'),
    ('20000000-0000-0000-0000-000000000009', 'en', 'Ready new build in Centrs — keys now', 'Class A, smart home, underground parking, concierge.'),
    -- p10: teika studio
    ('20000000-0000-0000-0000-000000000010', 'lv', 'Mājīga studija Teikā', 'Pēc remonta, iebūvēta virtuve, blakus tramvajs.'),
    ('20000000-0000-0000-0000-000000000010', 'en', 'Cozy studio in Teika', 'Renovated, built-in kitchen, tram nearby.'),
    -- p11: jugla house
    ('20000000-0000-0000-0000-000000000011', 'lv', 'Moderna māja Juglā, 2012. g.', 'Monolīta, siltināta, garāža, kopts dārzs.'),
    ('20000000-0000-0000-0000-000000000011', 'en', 'Modern house in Jugla, built 2012', 'Monolithic, insulated, garage, well-kept garden.'),
    -- p12: centrs stalinka (EN only)
    ('20000000-0000-0000-0000-000000000012', 'en', '2-room in Centrs — Stalinist building', 'High ceilings 3.1 m, parquet, stucco. Needs cosmetic work.'),
    -- p13: purvciems rental
    ('20000000-0000-0000-0000-000000000013', 'lv', '3-istabu Purvciemā ar mēbelēm', 'Pilnībā mēbelēts, balkons, kluss pagalms.'),
    ('20000000-0000-0000-0000-000000000013', 'en', '3-room in Purvciems with furniture', 'Fully furnished, balcony, quiet courtyard.');

-- ─────────────────────────────────────────────
-- Features
-- ─────────────────────────────────────────────
INSERT INTO property_features (property_id, feature) VALUES
    -- p01: centrs apartment
    ('20000000-0000-0000-0000-000000000001', 'balcony'),
    ('20000000-0000-0000-0000-000000000001', 'elevator'),
    -- p02: teika rental
    ('20000000-0000-0000-0000-000000000002', 'furnished'),
    ('20000000-0000-0000-0000-000000000002', 'pets'),
    -- p03: jurmala house
    ('20000000-0000-0000-0000-000000000003', 'parking'),
    -- p04: purvciems studio
    ('20000000-0000-0000-0000-000000000004', 'elevator'),
    ('20000000-0000-0000-0000-000000000004', 'balcony'),
    -- p05: agenskalns new_project
    ('20000000-0000-0000-0000-000000000005', 'new_building'),
    ('20000000-0000-0000-0000-000000000005', 'elevator'),
    ('20000000-0000-0000-0000-000000000005', 'balcony'),
    ('20000000-0000-0000-0000-000000000005', 'parking'),
    -- p06: imanta house
    ('20000000-0000-0000-0000-000000000006', 'parking'),
    ('20000000-0000-0000-0000-000000000006', 'balcony'),
    -- p07: ziepniekkalns rental
    ('20000000-0000-0000-0000-000000000007', 'pets'),
    -- p08: mezciems apartment
    ('20000000-0000-0000-0000-000000000008', 'balcony'),
    -- p09: centrs new_project ready
    ('20000000-0000-0000-0000-000000000009', 'new_building'),
    ('20000000-0000-0000-0000-000000000009', 'elevator'),
    ('20000000-0000-0000-0000-000000000009', 'balcony'),
    ('20000000-0000-0000-0000-000000000009', 'parking'),
    -- p10: teika studio
    ('20000000-0000-0000-0000-000000000010', 'furnished'),
    -- p11: jugla house
    ('20000000-0000-0000-0000-000000000011', 'parking'),
    ('20000000-0000-0000-0000-000000000011', 'balcony'),
    -- p12: centrs stalinka
    ('20000000-0000-0000-0000-000000000012', 'balcony'),
    ('20000000-0000-0000-0000-000000000012', 'elevator'),
    -- p13: purvciems rental
    ('20000000-0000-0000-0000-000000000013', 'furnished'),
    ('20000000-0000-0000-0000-000000000013', 'balcony');

-- ─────────────────────────────────────────────
-- Photos  (S3 fileUrl as returned by requestPresignedUrls → uploadFilesToS3)
-- position 0 = cover photo shown in listing cards
-- ─────────────────────────────────────────────
INSERT INTO property_photos (property_id, url, position) VALUES
    ('20000000-0000-0000-0000-000000000001', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000001/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000001', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000001/1.jpg', 1),
    ('20000000-0000-0000-0000-000000000001', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000001/2.jpg', 2),

    ('20000000-0000-0000-0000-000000000002', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000002/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000002', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000002/1.jpg', 1),

    ('20000000-0000-0000-0000-000000000003', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000003/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000003', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000003/1.jpg', 1),
    ('20000000-0000-0000-0000-000000000003', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000003/2.jpg', 2),

    ('20000000-0000-0000-0000-000000000004', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000004/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000004', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000004/1.jpg', 1),

    ('20000000-0000-0000-0000-000000000005', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000005/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000005', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000005/1.jpg', 1),
    ('20000000-0000-0000-0000-000000000005', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000005/2.jpg', 2),

    ('20000000-0000-0000-0000-000000000006', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000006/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000006', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000006/1.jpg', 1),
    ('20000000-0000-0000-0000-000000000006', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000006/2.jpg', 2),

    ('20000000-0000-0000-0000-000000000007', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000007/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000007', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000007/1.jpg', 1),

    ('20000000-0000-0000-0000-000000000008', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000008/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000008', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000008/1.jpg', 1),

    ('20000000-0000-0000-0000-000000000009', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000009/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000009', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000009/1.jpg', 1),
    ('20000000-0000-0000-0000-000000000009', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000009/2.jpg', 2),

    ('20000000-0000-0000-0000-000000000010', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000010/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000010', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000010/1.jpg', 1),

    ('20000000-0000-0000-0000-000000000011', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000011/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000011', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000011/1.jpg', 1),
    ('20000000-0000-0000-0000-000000000011', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000011/2.jpg', 2),

    ('20000000-0000-0000-0000-000000000012', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000012/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000012', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000012/1.jpg', 1),

    ('20000000-0000-0000-0000-000000000013', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000013/0.jpg', 0),
    ('20000000-0000-0000-0000-000000000013', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000013/1.jpg', 1),
    ('20000000-0000-0000-0000-000000000013', 'https://baltnami-media.s3.eu-north-1.amazonaws.com/photos/20000000-0000-0000-0000-000000000013/2.jpg', 2);

-- ─────────────────────────────────────────────
-- Phones
-- ─────────────────────────────────────────────
INSERT INTO property_phones (property_id, phone, position) VALUES
    -- p01
    ('20000000-0000-0000-0000-000000000001', '+371 29 123 456', 0),
    ('20000000-0000-0000-0000-000000000001', '+371 26 789 012', 1),
    -- p02
    ('20000000-0000-0000-0000-000000000002', '+371 29 234 567', 0),
    -- p03
    ('20000000-0000-0000-0000-000000000003', '+371 26 345 678', 0),
    ('20000000-0000-0000-0000-000000000003', '+371 29 456 789', 1),
    -- p04
    ('20000000-0000-0000-0000-000000000004', '+371 22 567 890', 0),
    -- p05
    ('20000000-0000-0000-0000-000000000005', '+371 29 678 901', 0),
    -- p06
    ('20000000-0000-0000-0000-000000000006', '+371 26 111 222', 0),
    ('20000000-0000-0000-0000-000000000006', '+371 29 333 444', 1),
    -- p07
    ('20000000-0000-0000-0000-000000000007', '+371 22 555 666', 0),
    -- p08
    ('20000000-0000-0000-0000-000000000008', '+371 29 777 888', 0),
    -- p09
    ('20000000-0000-0000-0000-000000000009', '+371 26 999 000', 0),
    ('20000000-0000-0000-0000-000000000009', '+371 29 112 233', 1),
    -- p10
    ('20000000-0000-0000-0000-000000000010', '+371 22 445 566', 0),
    -- p11
    ('20000000-0000-0000-0000-000000000011', '+371 29 667 788', 0),
    -- p12
    ('20000000-0000-0000-0000-000000000012', '+371 26 889 900', 0),
    -- p13
    ('20000000-0000-0000-0000-000000000013', '+371 29 223 344', 0),
    ('20000000-0000-0000-0000-000000000013', '+371 22 556 677', 1);

-- ─────────────────────────────────────────────
-- Saved listings
-- ─────────────────────────────────────────────
INSERT INTO saved_properties (user_id, property_id) VALUES
    -- u1 (Jānis) saved the new_project and the Jugla house
    ('10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000009'),
    ('10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000011'),
    -- u2 (Anna) saved the centrs apartment
    ('10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001'),
    -- u3 (Māris) saved the centrs new_project and the centrs stalinka
    ('10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000009'),
    ('10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001'),
    -- u4 (Laura) saved the Imanta house
    ('10000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000006'),
    -- u5 (Pēteris) saved the purvciems studio
    ('10000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000004');

COMMIT;