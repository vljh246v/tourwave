# FE Audit: Instructors

Last audit: 2026-04-18

## Summary
- Routes implemented: ❌ (seed state)
- Features module: ❌
- API client usage: ❌
- SSOT reference: openapi.yaml tag `Instructors`

## BE OpenAPI Paths (7 endpoints)

- `get /instructors/{instructorProfileId}` — Update instructor profile
- `get /organizations/{orgId}/instructor-registrations` — Request instructor registration
- `patch /me/instructor-profile` — Create instructor profile
- `post /instructor-registrations/{registrationId}/approve` — List instructor registrations
- `post /instructor-registrations/{registrationId}/reject` — Approve instructor registration
- `post /instructor-registrations` — Remove organization member
- `post /me/instructor-profile` — Reject instructor registration

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

