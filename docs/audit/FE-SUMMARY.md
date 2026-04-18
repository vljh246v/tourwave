# FE Audit Summary: tourwave-web

Audit Date: 2026-04-18
Status: Complete — all 23 OpenAPI tags analyzed

## Overview

The tourwave-web frontend is in **seed state**. As expected for an MVP bootstrap:
- No routes implemented beyond `/src/app/page.tsx` (landing page)
- No features modules created
- No API client or fetch wrapper
- No test runner configured
- No authentication state management

## OpenAPI Coverage

All 23 OpenAPI tags have been audited:

### Public/Guest Tier
1. **Search** — 1 endpoint (global occurrence search)
2. **Tours** — 11 endpoints (public tour catalog, detail, operators' tour management)
3. **Policies** — 1 endpoint (read-only: cancellation/refund policies)

### Authenticated User Tier
4. **Auth** — 8 endpoints (login, signup, password reset, email verification, token refresh)
5. **Me** — 4 endpoints (user profile, deactivation, deletion)
6. **Bookings** — 14 endpoints (create, approve/reject, state transitions, party size, attendance)
7. **Bookings** detail patterns: offers, expiration, participant management
8. **Favorites** — 3 endpoints (list, add/remove favorites)
9. **Inquiries** — 6 endpoints (create ticket, list, add messages, close)
10. **Notifications** — 3 endpoints (list, mark read, mark all read)
11. **Participants** — 4 endpoints (list, accept/decline invitations)
12. **Waitlist** — 2 endpoints (list, manual promote)
13. **Reviews** — 9 endpoints (tour/instructor/organization reviews, summary views)

### Operator (ORG_ADMIN/ORG_OWNER) Tier
14. **Organizations** — 6 endpoints (create, detail, member management)
15. **Instructors** — 7 endpoints (registration, approval, profile management)
16. **Occurrences** — 10 endpoints (create, publish, reschedule, cancel, finish, notes)
17. **Occurrences** detail: availability check, quote, notification, policies
18. **Announcements** — 5 endpoints (create, update, list)
19. **Assets** — 7 endpoints (upload, attach to tours, reorder, delete)
20. **Calendar** — 2 endpoints (.ics export for bookings and occurrences)
21. **Notes** — 1 endpoint (add operator notes to occurrences)
22. **Reports** — 4 endpoints (booking/occurrence reports, CSV export)
23. **Finance** (operator-only) — 3 endpoints (reconciliation daily, refresh, export)
24. **Operations** (operator-only) — 2 endpoints (remediation queue for cross-surface issues)
25. **Payments** (operator/webhook) — 3 endpoints (refund retry, refunds queue, webhook receiver)

## Cross-Cutting Blockers (High Priority)

These MUST be implemented before ANY feature can function:

1. **API Client Layer** (`src/lib/api/client.ts`)
   - fetch wrapper with Bearer token injection
   - Error normalization (400, 401, 403, 422, 409, 5xx handling)
   - Retry logic for idempotent operations
   - Request/response logging

2. **Type Definitions** (`src/lib/api/schema.ts`)
   - Auto-generated from openapi.yaml (use openapi-generator or similar)
   - Request/response DTOs for all 23 tags
   - Enum types for status values, roles, error codes

3. **Authentication Module** (`src/lib/auth/`)
   - JWT storage adapter (localStorage/sessionStorage abstraction)
   - Token refresh on 401 (silent refresh flow)
   - Token expiration detection
   - Logout cleanup

4. **Auth Hook** (`src/lib/hooks/useAuth.ts`)
   - Global auth state (Context API or Zustand)
   - User info, roles, permissions
   - Login/logout/signup handlers
   - Token injection into all API calls

5. **Route Protection** (`src/middleware.ts`)
   - Redirect unauthenticated users to /login
   - Role-based route guards (guest, user, operator)
   - Token validation before protected routes

## Phase-Based Implementation Path

### Phase 1: Cross-Cutting (Weeks 1-2)
- [ ] Setup API client + error handling
- [ ] Generate type definitions
- [ ] Implement JWT storage & auth hook
- [ ] Add route protection middleware
- [ ] Add test harness (Vitest + MSW)

### Phase 2: Public Tier (Week 3)
- [ ] `/` — landing page (refactor from seed)
- [ ] `/tours` — public tour catalog (GET /tours)
- [ ] `/tours/:id` — tour detail (GET /tours/:id)
- [ ] `/search` — global search (GET /search/occurrences)
- [ ] `/policies` — read-only policy pages

### Phase 3: Auth Tier (Weeks 4-5)
- [ ] `/login` — login form + flow
- [ ] `/signup` — signup form + email verification
- [ ] `/password-reset` — password reset flow (2 steps)
- [ ] `/me` — user profile (GET /me, PATCH /me)
- [ ] `/me/settings` — deactivate/delete account

### Phase 4: Core Booking Flow (Week 6-7)
- [ ] `/bookings` — my bookings list (GET /me/bookings)
- [ ] `/bookings/:id` — booking detail (GET /bookings/:id)
- [ ] `/search` → booking creation flow (POST /occurrences/:id/bookings)
- [ ] Booking state transitions: approve, reject, cancel
- [ ] Offer accept/decline
- [ ] Participant management

### Phase 5: Operator Dashboard (Week 8+)
- [ ] `/operator/tours` — tour management
- [ ] `/operator/occurrences` — schedule management
- [ ] `/operator/bookings` — booking admin view
- [ ] `/operator/instructors` — instructor registration flow
- [ ] `/operator/reports` — analytics & CSV export
- [ ] `/operator/finance` — reconciliation dashboard

## UI Component Taxonomy

### Reusable Components
- `<Button>` — CTA with loading/error states
- `<Modal>` — confirm dialogs, forms
- `<Form>` — field validation, error display
- `<List>` — pagination, filtering, sorting
- `<Card>` — tour/booking/occurrence preview
- `<Badge>` — status indicators (DRAFT, PUBLISHED, PENDING, etc.)
- `<Tabs>` — navigation within pages
- `<Dropdown>` — role/org switching, notifications

### Layout Templates
- `AuthLayout` — minimal (no sidebar)
- `AppLayout` — sidebar + main (authenticated user)
- `OperatorLayout` — dual-nav (for org switching)

### Feature Modules by Tag
Each tag gets a feature folder:
```
src/features/
├── auth/
├── tours/
├── bookings/
├── inquiries/
├── occurrences/
├── organizations/
├── ... (one per tag)
```

## Critical Domain Rules to Enforce in UI

1. **Idempotency-Key Header** (Bookings, Participants, Inquiries, Announcements)
   - Required on all POST/PATCH state changes
   - Generate: `uuid()` on form submit
   - Store in optimistic update cache key

2. **Booking State Machine**
   ```
   PENDING -> [APPROVED | REJECTED | EXPIRED]
   APPROVED -> [COMPLETED | CANCELED]
   OFFERED -> [ACCEPTED (via offer/accept) | DECLINED]
   ```
   - No backward transitions
   - UI must disable invalid actions

3. **Seat Constraints**
   - Confirm reserved seats + offered seats ≤ capacity
   - Show real-time availability before booking
   - Block if capacity exhausted

4. **Timezone Handling**
   - All times displayed in user's local TZ
   - All stored as UTC in DB (timestamp_utc)
   - Occurrence policies calculated in occurrence TZ (IANA)

5. **Role-Based Visibility**
   - Guest: public tours, policies
   - User: bookings, favorites, inquiries, reviews
   - ORG_ADMIN/ORG_OWNER: all operator endpoints
   - `me` endpoints always return current user

## Testing Strategy

| Tier | Tool | Scope |
|------|------|-------|
| Component | Vitest + React Testing Library | Single component in isolation |
| Feature | Vitest + MSW (Mock Service Worker) | Feature module with mocked API |
| E2E | Playwright | Full user flow (login → booking) |
| Accessibility | axe-core / Playwright | WCAG AA compliance |

## Known Limitations (Seed State)

- No offline support (PWA planned for Phase 6)
- No real-time updates via WebSocket (polling only for Notifications)
- No image optimization (next/image planned)
- No analytics/error tracking (Sentry planned)
- No i18n (English only for MVP)

## Files Generated

All audit files located in `/docs/audit/`:

```
FE-announcements.md
FE-assets.md
FE-auth.md
FE-bookings.md
FE-calendar.md
FE-favorites.md
FE-finance.md
FE-inquiries.md
FE-instructors.md
FE-me.md
FE-notes.md
FE-notifications.md
FE-occurrences.md
FE-operations.md
FE-organizations.md
FE-participants.md
FE-payments.md
FE-policies.md
FE-reports.md
FE-reviews.md
FE-search.md
FE-tours.md
FE-waitlist.md
```

Each file contains:
- BE OpenAPI path list (method, path, summary)
- Current FE state (routes, modules, API client)
- Recommended UI components (tag-specific)
- Domain constraints (tag-specific)
- Cross-cutting implementation checklist

## Next Steps

1. **Immediate** (Phase 1): Implement cross-cutting auth + API layers
2. **Short-term** (Phase 2-3): Public tier + login flow
3. **Medium-term** (Phase 4-5): Core booking domain
4. **Long-term** (Phase 5+): Operator dashboard
5. **Ongoing**: Sync FE types with BE OpenAPI via CI (contract testing)

---

**BE SSOT:** `/docs/openapi.yaml`
**FE Repo:** `tourwave-web` (separate from `tourwave` BE)
**Audit Contact:** Harness Phase 1 completion review
