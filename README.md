# Baltnami

Open-source real estate marketplace for Latvia

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3, TypeScript, Tailwind CSS v4, Vite |
| Backend | Java 25, Spring Boot 4, Gradle |
| Database | PostgreSQL 17 (pgcrypto) |
| Storage | AWS S3 + CloudFront CDN |
| Email | Resend |
| Auth | JWT in HttpOnly access + refresh cookies |

## Features

- Property listings: buy, rent, new projects - apartments and houses
- Multilingual: Latvian, English, Russian
- Photo and floor plan uploads (direct-to-S3 via presigned URLs)
- Geospatial proximity search
- Saved listings, user profiles, GDPR data export
- Email verification, password reset
- Listing expiry and renewal lifecycle
- Duplicate property address detection on listing creation
- Bot protection on listing creation (Cloudflare Turnstile)
- Rate limiting on auth, upload, and property write endpoints
- CSRF protection on state-changing requests (`SameSite=Lax` cookies plus a double-submit `XSRF-TOKEN`)

## Prerequisites

- Java 25 JDK
- Node.js 22+
- Docker (for running tests with Testcontainers)
- PostgreSQL 17

## Local Setup

A `Makefile` at the repo root wraps the commands below — run `make help` to list all targets.

### 1. Database

Create a local PostgreSQL 17 database and run the schema:

```bash
make db-setup       # createdb + pgcrypto + schema
make db-seed        # optional: load sample data
```

Equivalent raw commands:

```bash
psql -U postgres -c "CREATE DATABASE baltnami;"
psql -U postgres -d baltnami -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
psql -U postgres -d baltnami -f backend/db/schema.sql
# Optional: load sample data
psql -U postgres -d baltnami -f backend/db/seed.sql
```

### 2. Backend

```bash
cd backend
cp .env.example .env
# Edit .env with your values
cd ..
make backend-run
```

Backend runs on `http://localhost:8080`.

### 3. Frontend

```bash
make frontend-install
make frontend-dev
```

Frontend runs on `http://localhost:5173`. The Vite dev server proxies `/api/*` to the backend automatically.

To run without the backend (mock API):

```bash
VITE_MOCK=true npm run dev
```

## Environment Variables

All backend config is driven by environment variables. See `backend/.env.example` for the full list with descriptions.

Key variables for production:

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC connection string |
| `JWT_SECRET` | HMAC signing key (min 32 chars) |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | S3 upload credentials |
| `S3_BUCKET` / `CDN_URL` | Storage bucket and CloudFront URL |
| `RESEND_API_KEY` | Transactional email |
| `CORS_ALLOWED_ORIGINS` | Frontend origin (e.g. `https://your-app.vercel.app`) |
| `FRONTEND_URL` | Used in email links |
| `COOKIE_SECURE` | Set `true` in production (requires HTTPS) |
| `TURNSTILE_SECRET_KEY` | Cloudflare Turnstile secret key (bot protection on listing creation); leave unset to skip verification |

For the frontend, set `VITE_API_BASE_URL` to your backend URL in production (e.g. `https://your-backend.up.railway.app`). In local dev, leave it unset — the Vite proxy handles routing.

Set `VITE_TURNSTILE_SITE_KEY` to your Cloudflare Turnstile site key to render the CAPTCHA widget on the add-listing form; leave it unset to disable it (matches `TURNSTILE_SECRET_KEY` unset on the backend).

## Running Tests

```bash
# Backend (requires Docker for Testcontainers)
# Runs tests + JaCoCo coverage check (minimum 70% line coverage)
make backend-check
```

## Deployment

Deployment is fully Git-driven — no manual build or push steps required.

| Layer | Platform | Trigger |
|---|---|---|
| Frontend | Vercel | Push to `main` — Vercel builds and deploys automatically |
| Backend | Railway | Push to `main` — Railway reads `backend/Dockerfile` and builds on their infrastructure |
| Database | Neon | Managed Postgres 17; `DB_URL` points at it — no deploy trigger |

Configure the [environment variables](#environment-variables) in each platform's dashboard. No GitHub Actions runner minutes are consumed by deployment.
