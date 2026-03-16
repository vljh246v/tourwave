# Tour Booking Platform (MVP) — Overview

## Goal
Build a mobile-first booking platform for **Tours/Activities** (e.g., scuba diving, skydiving) where:
- Users search **TourOccurrences** (actual schedules) and submit **Bookings**.
- Bookings are **approved by Organization operators** (ORG_ADMIN/ORG_OWNER).
- Capacity limits exist; **Waitlist** is supported with **24h offer window** (operator can manage manually too).
- A booking can include a **partySize** and **participant invitations**.
- Users can create **Inquiry tickets** tied to a specific booking/tour.
- After tour completion, **reviews** are allowed (content private, rating/count public).

## Key Concepts (Glossary)
- **Tour (Template)**: A reusable tour listing (title, locationText, category, format, pricing).
- **TourOccurrence**: A scheduled instance of a Tour (startsAt/endsAt/timezone/capacity/status).
- **Organization (Org)**: Entity that owns Tours/Occurrences and operates approvals.
- **Roles (set)**: USER / INSTRUCTOR / ORG_MEMBER / ORG_ADMIN / ORG_OWNER.
- **Booking**: A request to join a TourOccurrence with a partySize.
- **Waitlist Offer**: When seats open, a WAITLISTED booking becomes OFFERED and the leader has 24h to accept/decline.
- **BookingParticipant**: Leader + invited members; invitations expire and can be re-sent.
- **Inquiry (투어 상담 티켓)**: Ticket-like conversation tied to a booking; participants + org operators can post messages; attachments supported.
- **Attendance**: Check-in/attendance record; needed for review eligibility.
- **Reviews**: Two types: TourReview and InstructorReview. **Content restricted** (author/instructor/org only). Public can see **rating/count summary only**.

## Time / Locale
- Store all timestamps as **UTC**.
- Keep an occurrence’s **timezone (IANA)** for local rendering.
- Multi-day tours are supported via startsAtUtc/endsAtUtc on the occurrence.
- i18n: MVP can be single-language; later add translations for content.

## Non-goals (MVP)
- Real payment gateway integration (we use **payment stub states**).
- Complex partial refunds / prorations (MVP: full refund or non-refundable).
- Multi-organization ownership per tour (MVP: one org per tour).
- Public review content (MVP: public summary only).
- Webhooks / external integrations (deferred).

## Core Deliverables
- MySQL schema (FK minimized; business logic enforces constraints).
- REST API + OpenAPI spec.
- Authorization model and guards.
- Background jobs (offer/invite expiry, reconcile).
- Search (MVP: MySQL-based; upgrade path to ES/OpenSearch).

## Spec Governance (for implementation)
- Domain source of truth: `01_domain_rules.md`
- Operational policy tables: `08_operational_policy_tables.md`
- API contract source of truth: `04_openapi.yaml`
- AuthZ source of truth: `05_authz_model.md`

Implementation teams should treat `08_operational_policy_tables.md` as required detail for:
- refund/cancellation boundary behavior
- idempotency and duplicate-request handling
- timezone boundary rules (6h/48h/offer expiry)
- audit/payment compensation operations

## Quick Entry
- Spec governance and reading order: `09_spec_index.md`
