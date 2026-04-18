# FE Audit: Bookings

Last audit: 2026-04-18

## Summary
- Routes implemented: ❌ (seed state)
- Features module: ❌
- API client usage: ❌
- SSOT reference: openapi.yaml tag `Bookings`

## BE OpenAPI Paths (14 endpoints)

- `get /bookings/{bookingId}/explain` — Force expire offered booking (ORG_ADMIN/ORG_OWNER)
- `get /bookings/{bookingId}` — Approve booking request
- `get /me/bookings` — Delete account (soft delete)
- `patch /bookings/{bookingId}/party-size` — Decline waitlist offer (leader)
- `post /bookings/{bookingId}/approve` — Reject booking request
- `post /bookings/{bookingId}/cancel` — Get booking detail
- `post /bookings/{bookingId}/complete` — Reduce booking party size (leader only)
- `post /bookings/{bookingId}/extend-offer` — Update participant attendance (ORG_ADMIN/ORG_OWNER)
- `post /bookings/{bookingId}/force-expire` — Extend offer expiration (ORG_ADMIN/ORG_OWNER)
- `post /bookings/{bookingId}/offer/accept` — Cancel booking
- `post /bookings/{bookingId}/offer/decline` — Accept waitlist offer (leader)
- `post /bookings/{bookingId}/reject` — Create booking request
- `post /occurrences/{occurrenceId}/bookings` — Delete announcement (ORG_ADMIN/ORG_OWNER)
- `put /bookings/{bookingId}/attendance` — Complete booking (ORG_ADMIN/ORG_OWNER)

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

- Booking state machine implementation required (PENDING -> APPROVED -> COMPLETED)
- Idempotency-Key header required (POST/PATCH)
- Real-time seat availability updates

## Cross-cutting implementation required

- [ ] src/lib/api/client.ts — fetch wrapper (Bearer token, error handling)
- [ ] src/lib/api/schema.ts — type definitions for all endpoints
- [ ] src/lib/auth/ — JWT storage (localStorage/sessionStorage)
- [ ] src/lib/hooks/useAuth.ts — auth state management (Context API or Zustand)
- [ ] src/middleware.ts — route protection (auth check)

