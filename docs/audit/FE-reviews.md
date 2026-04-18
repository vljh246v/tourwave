# FE Audit: Reviews

Last audit: 2026-04-18

## Summary
- Routes implemented: ❌ (seed state)
- Features module: ❌
- API client usage: ❌
- SSOT reference: openapi.yaml tag `Reviews`

## BE OpenAPI Paths (9 endpoints)

- `get /instructors/{instructorProfileId}/reviews/summary` — /instructors/{instructorProfileId}/reviews/summary:
- `get /occurrences/{occurrenceId}/reviews/summary` — /occurrences/{occurrenceId}/reviews/summary:
- `get /operator/organizations/{orgId}/reviews/summary` — /operator/organizations/{orgId}/reviews/summary:
- `get /organizations/{orgId}/reviews/summary` — /organizations/{orgId}/reviews/summary:
- `get /organizations/{orgId}/reviews` — Get review (restricted)
- `get /reviews/{reviewId}` — Occurrence review summary
- `get /tours/{tourId}/reviews/summary` — /tours/{tourId}/reviews/summary:
- `post /occurrences/{occurrenceId}/reviews/instructor` — Create tour review (ATTENDED only)
- `post /occurrences/{occurrenceId}/reviews/tour` — Close inquiry ticket

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

- [ ] List page
- [ ] Detail page
- [ ] Create/Edit form
- [ ] Delete confirmation modal

## Observed Issues

- tourwave-web is in seed state

## Cross-cutting implementation required

- [ ] src/lib/api/client.ts — fetch wrapper (Bearer token, error handling)
- [ ] src/lib/api/schema.ts — type definitions for all endpoints
- [ ] src/lib/auth/ — JWT storage (localStorage/sessionStorage)
- [ ] src/lib/hooks/useAuth.ts — auth state management (Context API or Zustand)
- [ ] src/middleware.ts — route protection (auth check)

