# FE Audit: Participants

Last audit: 2026-04-18

## Summary
- Routes implemented: ❌ (seed state)
- Features module: ❌
- API client usage: ❌
- SSOT reference: openapi.yaml tag `Participants`

## BE OpenAPI Paths (4 endpoints)

- `get /bookings/{bookingId}/participants` — Booking calendar ICS
- `post /bookings/{bookingId}/participants` — List booking participants
- `post /participants/{participantId}/accept` — Add participant (invite)
- `post /participants/{participantId}/decline` — Accept invitation

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

