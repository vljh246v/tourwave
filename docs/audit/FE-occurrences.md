# FE Audit: Occurrences

Last audit: 2026-04-18

## Summary
- Routes implemented: ❌ (seed state)
- Features module: ❌
- API client usage: ❌
- SSOT reference: openapi.yaml tag `Occurrences`

## BE OpenAPI Paths (10 endpoints)

- `get /occurrences/{occurrenceId}/availability` — Notify participants (ORG_ADMIN/ORG_OWNER)
- `get /occurrences/{occurrenceId}/quote` — Check availability for party size
- `get /occurrences/{occurrenceId}` — Create occurrence (ORG_ADMIN/ORG_OWNER)
- `get /tours/{tourId}/occurrences` — Unfavorite a tour
- `patch /occurrences/{occurrenceId}` — Get occurrence detail
- `post /occurrences/{occurrenceId}/cancel` — Update occurrence (ORG_ADMIN/ORG_OWNER)
- `post /occurrences/{occurrenceId}/finish` — Cancel occurrence (ORG_ADMIN/ORG_OWNER)
- `post /occurrences/{occurrenceId}/notify` — Reschedule occurrence (ORG_ADMIN/ORG_OWNER)
- `post /occurrences/{occurrenceId}/reschedule` — Mark occurrence finished (ORG_ADMIN/ORG_OWNER)
- `post /tours/{tourId}/occurrences` — List occurrences for a tour (public if published)

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

