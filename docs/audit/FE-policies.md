# FE Audit: Policies

Last audit: 2026-04-18

## Summary
- Routes implemented: ❌ (seed state)
- Features module: ❌
- API client usage: ❌
- SSOT reference: openapi.yaml tag `Policies`

## BE OpenAPI Paths (2 endpoints)

- `get /occurrences/{occurrenceId}/policies` — Add operator note
- `put /occurrences/{occurrenceId}/policies` — Get cancellation policy

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

- [ ] Read-only policy page (refund, cancellation)
- [ ] Policy detail view
- [ ] Text-only layout

## Observed Issues

- Read-only content (managed by admins)
- Consider Markdown or Rich Text support

## Cross-cutting implementation required

- [ ] src/lib/api/client.ts — fetch wrapper (Bearer token, error handling)
- [ ] src/lib/api/schema.ts — type definitions for all endpoints
- [ ] src/lib/auth/ — JWT storage (localStorage/sessionStorage)
- [ ] src/lib/hooks/useAuth.ts — auth state management (Context API or Zustand)
- [ ] src/middleware.ts — route protection (auth check)

