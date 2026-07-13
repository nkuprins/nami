-- Migration: listings.phones gains per-entry name/email (run once against an
-- existing database; db/schema.sql already reflects this shape for fresh
-- installs). Converts each phone entry from a plain string to
-- {"phone": ..., "name": ..., "email": ...}, defaulting name/email to the
-- listing's owner — matching PropertyMapper#applyListingContent's fallback for
-- entries left blank going forward.

UPDATE listings
SET phones = (
    SELECT jsonb_agg(jsonb_build_object(
        'phone', elem,
        'name', u.name,
        'email', u.email
    ))
    FROM jsonb_array_elements_text(listings.phones) AS elem
)
FROM users u
WHERE u.id = listings.owner_id
  AND jsonb_typeof(listings.phones) = 'array'
  AND jsonb_array_length(listings.phones) > 0
  AND jsonb_typeof(listings.phones -> 0) = 'string';
