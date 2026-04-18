# FE Audit: Notifications

Last audit: 2026-04-18

## Summary
- Routes implemented: ❌ (seed state)
- Features module: ❌
- API client usage: ❌
- SSOT reference: openapi.yaml tag `Notifications`

## BE OpenAPI Paths (3 endpoints)

- `get /me/notifications` — List my favorite tours
- `post /me/notifications/{notificationId}/read` — List my notifications
- `post /me/notifications/read-all` — Mark notification read

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

- [ ] Header dropdown notification display
- [ ] Dedicated notifications page (list + filters)
- [ ] Mark individual notification as read (POST /me/notifications/{id}/read)
- [ ] Mark all as read button

## Observed Issues

- WebSocket or polling required (real-time notifications)
- Display unread notification badge

## Cross-cutting implementation required

- [ ] src/lib/api/client.ts — fetch wrapper (Bearer token, error handling)
- [ ] src/lib/api/schema.ts — type definitions for all endpoints
- [ ] src/lib/auth/ — JWT storage (localStorage/sessionStorage)
- [ ] src/lib/hooks/useAuth.ts — auth state management (Context API or Zustand)
- [ ] src/middleware.ts — route protection (auth check)

