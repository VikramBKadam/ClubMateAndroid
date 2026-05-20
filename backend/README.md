# ClubMates Backend

Fastify + TypeScript API backed by Supabase Postgres.

## Setup

```bash
cd backend
nvm use
npm install
cp .env.example .env
npm run migrate
npm run dev
```

Set `DATABASE_URL` to the Supabase Postgres connection string from Project Settings > Database. Keep server-only secrets in `backend/.env`; the iOS app should only talk to this API.

## Current Scope

- Phone OTP endpoints with database-backed dev OTP storage.
- JWT access tokens and refresh-token rotation.
- Profile CRUD, venues/check-ins, discovery, swipes/matches, messages, blocks, and reports.
- Postgres migrations matching `BACKEND_ARCHITECTURE.md`.

Redis, MSG91/Twilio, APNs, Supabase Storage image processing, and WebSocket chat are intentionally left as next integration layers.
