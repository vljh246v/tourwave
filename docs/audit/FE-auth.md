# FE Audit: Auth

Last audit: 2026-04-18

## Summary
- Routes implemented: ❌ (seed state)
- Features module: ❌
- API client usage: ❌
- SSOT reference: openapi.yaml tag `Auth`

## BE OpenAPI Paths (8 endpoints)

- `post /auth/email/verify-confirm` — Request email verification
- `post /auth/email/verify-request` — Confirm password reset
- `post /auth/login` — Sign up
- `post /auth/logout` — Login
- `post /auth/password/reset-confirm` — Request password reset email
- `post /auth/password/reset-request` — Refresh token
- `post /auth/refresh` — Logout
- `post /auth/signup` — Complete upload (finalize asset)

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

- [ ] Login form (email, password)
- [ ] Signup form (with email verification)
- [ ] Password reset request form
- [ ] Password reset confirm form
- [ ] Email verification confirm screen

## Observed Issues

- JWT token must be stored in localStorage/sessionStorage after login
- Auth state must persist after page refresh (token validation)
- Logout must clear token and redirect to /login

## Cross-cutting implementation required

- [ ] src/lib/api/client.ts — fetch wrapper (Bearer token, error handling)
- [ ] src/lib/api/schema.ts — type definitions for all endpoints
- [ ] src/lib/auth/ — JWT storage (localStorage/sessionStorage)
- [ ] src/lib/hooks/useAuth.ts — auth state management (Context API or Zustand)
- [ ] src/middleware.ts — route protection (auth check)

