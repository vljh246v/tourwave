# FE Audit: Finance

Last audit: 2026-04-18

## Summary
- Routes implemented: ❌ (seed state)
- Features module: ❌
- API client usage: ❌
- SSOT reference: openapi.yaml tag `Finance`

## BE OpenAPI Paths (3 endpoints)

- `get /operator/finance/reconciliation/daily/export` — Refresh one daily reconciliation summary
- `get /operator/finance/reconciliation/daily` — Retry refund for a booking
- `post /operator/finance/reconciliation/daily/{summaryDate}/refresh` — List daily reconciliation summaries

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

