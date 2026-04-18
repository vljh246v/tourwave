# FE Audit: Inquiries

Last audit: 2026-04-18

## Summary
- Routes implemented: ❌ (seed state)
- Features module: ❌
- API client usage: ❌
- SSOT reference: openapi.yaml tag `Inquiries`

## BE OpenAPI Paths (6 endpoints)

- `get /inquiries/{inquiryId}/messages` — Get inquiry ticket
- `get /inquiries/{inquiryId}` — Create tour inquiry ticket
- `get /me/inquiries` — List my bookings
- `post /inquiries/{inquiryId}/close` — Post inquiry message (with optional attachments)
- `post /inquiries/{inquiryId}/messages` — List inquiry messages
- `post /occurrences/{occurrenceId}/inquiries` — Set cancellation policy

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

