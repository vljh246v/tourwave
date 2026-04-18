# FE Audit: Tours

Last audit: 2026-04-18

## Summary
- Routes implemented: ❌ (seed state)
- Features module: ❌
- API client usage: ❌
- SSOT reference: openapi.yaml tag `Tours`

## BE OpenAPI Paths (11 endpoints)

- `get /organizations/{orgId}/tours` — Get instructor review summary
- `get /tours/{tourId}/content` — Set tour instructors (ORG_ADMIN/ORG_OWNER)
- `get /tours/{tourId}/instructors` — Archive tour (PUBLISHED -> ARCHIVED) (ORG_ADMIN/ORG_OWNER)
- `get /tours/{tourId}` — Public tour list (published only)
- `get /tours` — Create tour (ORG_ADMIN/ORG_OWNER)
- `patch /tours/{tourId}` — Get tour detail (public if published; operator if same org)
- `post /organizations/{orgId}/tours` — List tours for org (ORG_ADMIN/ORG_OWNER)
- `post /tours/{tourId}/archive` — Publish tour (DRAFT -> PUBLISHED) (ORG_ADMIN/ORG_OWNER)
- `post /tours/{tourId}/publish` — Update tour (ORG_ADMIN/ORG_OWNER)
- `put /tours/{tourId}/content` — Get tour rich content (public if published; operator if same org)
- `put /tours/{tourId}/instructors` — Get tour instructors (public)

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

- Tour card list view
- Tour publish/archive state management
- Asset management (image upload and reordering)

## Cross-cutting implementation required

- [ ] src/lib/api/client.ts — fetch wrapper (Bearer token, error handling)
- [ ] src/lib/api/schema.ts — type definitions for all endpoints
- [ ] src/lib/auth/ — JWT storage (localStorage/sessionStorage)
- [ ] src/lib/hooks/useAuth.ts — auth state management (Context API or Zustand)
- [ ] src/middleware.ts — route protection (auth check)

