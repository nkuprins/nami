# Baltnami

Real estate marketplace for Latvia — browse, post, and manage property listings across buying, renting, and new development projects.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3, TypeScript, Tailwind CSS v4, Vite |
| Backend | Java 25, Spring Boot 4, Gradle |
| Database | PostgreSQL 17 (pgcrypto, cube, earthdistance) |
| Storage | AWS S3 + CloudFront CDN |
| Email | Resend |
| Auth | JWT (access token) + HttpOnly refresh cookie |

## Features

- Property listings: buy, rent, new projects — apartments and houses
- Multilingual: Latvian, English, Russian
- Photo and floor plan uploads (direct-to-S3 via presigned URLs)
- Geospatial proximity search
- Saved listings, user profiles, GDPR data export
- Email verification, password reset
- Listing expiry and renewal lifecycle
- Rate limiting on auth endpoints

## Prerequisites

- Java 25 JDK
- Node.js 22+
- Docker (for running tests with Testcontainers)
- PostgreSQL 17

## Local Setup

### 1. Database

Create a local PostgreSQL 17 database and run the schema:

```bash
psql -U postgres -c "CREATE DATABASE baltnami;"
psql -U postgres -d baltnami -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
psql -U postgres -d baltnami -c "CREATE EXTENSION IF NOT EXISTS cube;"
psql -U postgres -d baltnami -c "CREATE EXTENSION IF NOT EXISTS earthdistance;"
psql -U postgres -d baltnami -f backend/db/schema.sql
# Optional: load sample data
psql -U postgres -d baltnami -f backend/db/seed.sql
```

### 2. Backend

```bash
cd backend
cp .env.example .env
# Edit .env with your values
./gradlew bootRun
```

Backend runs on `http://localhost:8080`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
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

For the frontend, set `VITE_API_BASE_URL` to your backend URL in production (e.g. `https://your-backend.up.railway.app`). In local dev, leave it unset — the Vite proxy handles routing.

## Running Tests

```bash
# Backend (requires Docker for Testcontainers)
cd backend && ./gradlew test
```