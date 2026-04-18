# FE Audit: Me

Last audit: 2026-04-18

## Summary
- Routes implemented: ❌ (seed state)
- Features module: ❌
- API client usage: ❌
- SSOT reference: openapi.yaml tag `Me`

## BE OpenAPI Paths (4 endpoints)

- `get /me` — Confirm email verification
- `patch /me` — Get current user
- `post /me/deactivate` — Update current user
- `post /me/delete` — Deactivate account (soft)

## Current FE State

### Routes (src/app/)
- ❌ Not implemented (seed state)

### Features module (src/features/)
- ❌ Directory does not exist

### API integration (src/lib/api/)
- ❌ Schema definitions missing
- ❌ fetch wrapper missing

### Tests
- ❌ Test runner not configured

## Recommended UI Components (Phase 3 hint)

- [ ] Profile page (GET /me)
- [ ] Profile edit form (PATCH /me)
- [ ] Account deactivation confirm modal (POST /me/deactivate)
- [ ] Account deletion confirm modal (POST /me/delete)

## Observed Issues

- Authenticated user profile only (auth required)
- Account deactivation/deletion requires reconfirmation

## Cross-cutting implementation required

- [ ] src/lib/api/client.ts — fetch wrapper (Bearer token, error handling)
- [ ] src/lib/api/schema.ts — type definitions for all endpoints
- [ ] src/lib/auth/ — JWT storage (localStorage/sessionStorage)
- [ ] src/lib/hooks/useAuth.ts — auth state management (Context API or Zustand)
- [ ] src/middleware.ts — route protection (auth check)

