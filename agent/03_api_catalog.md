# API Catalog (MVP) — Endpoints + Auth + Key Errors

> 현재 구현 상태는 `13_api_status_matrix.md`를 먼저 확인한다.
> 이 문서는 제품 목표 API 카탈로그이며, 일부 항목은 아직 미구현 상태일 수 있다.

## Conventions
- Target Product Auth: Bearer JWT (except public)
- Current Runtime Auth: Bearer JWT, with request header actor context fallback only in local/test flows
- Pagination: cursor + limit
- Time: UTC timestamps in API; occurrence includes timezone(IANA)
- Error: { error: { code, message, details? } }

이 문서는 제품 목표 API 카탈로그다. 현재 실제 구현 상태는 `13_api_status_matrix.md`를 우선한다.

### Contract Update Order
- 1차 확인: controller + integration test
- 2차 반영: `13_api_status_matrix.md`
- 3차 반영: `04_openapi.yaml`
- 4차 반영: `03_api_catalog.md`와 handoff 문서

즉, 현재 문서는 target catalog이고, current runtime truth를 직접 대체하지 않는다.

### Mutation Safety (OpenAPI Sync)
- All domain state-changing endpoints MUST require `Idempotency-Key` header.
- Dedup scope: `(actor_user_id, method, path_template, idempotency_key)`.
- If same key + different payload: `422 IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD`.
- If same key still processing: `409 IDEMPOTENCY_IN_PROGRESS`.

### Idempotency Required Endpoints (MVP)
- POST /occurrences/{occurrenceId}/bookings
- POST /bookings/{bookingId}/approve
- POST /bookings/{bookingId}/reject
- POST /bookings/{bookingId}/cancel
- POST /bookings/{bookingId}/offer/accept
- POST /bookings/{bookingId}/offer/decline
- PATCH /bookings/{bookingId}/party-size
- POST /occurrences/{occurrenceId}/inquiries

### Representative Error Mapping (409 / 422)

| HTTP | code | Meaning | Typical endpoint |
|---|---|---|---|
| 409 | INVALID_STATE_TRANSITION | Transition is not allowed from current state | POST /bookings/{bookingId}/approve |
| 409 | BOOKING_TERMINAL_STATE | Command targets terminal booking | POST /bookings/{bookingId}/cancel |
| 409 | OFFER_NOT_ACTIVE | Offer command called outside OFFERED state | POST /bookings/{bookingId}/offer/accept |
| 409 | OFFER_EXPIRED | Offer expired by time boundary | POST /bookings/{bookingId}/offer/accept |
| 409 | CAPACITY_EXCEEDED | Seat allocation exceeds available capacity | POST /bookings/{bookingId}/offer/accept |
| 409 | IDEMPOTENCY_IN_PROGRESS | Same idempotency key currently processing | POST /occurrences/{occurrenceId}/bookings |
| 422 | BOOKING_SCOPE_MISMATCH | bookingId does not match occurrence/org scope | POST /occurrences/{occurrenceId}/inquiries |
| 422 | REQUIRED_FIELD_MISSING | Required field missing (e.g., bookingId) | POST /occurrences/{occurrenceId}/inquiries |
| 422 | PARTY_SIZE_INCREASE_NOT_ALLOWED | party-size patch tries to increase | PATCH /bookings/{bookingId}/party-size |
| 422 | IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD | Same key reused with different body | POST /occurrences/{occurrenceId}/bookings |

---

## Auth / Account
- POST /auth/signup (public)
- POST /auth/login (public)
- POST /auth/logout (auth)
- POST /auth/refresh (token flow)

- POST /auth/password/reset-request (public)
- POST /auth/password/reset-confirm (public)
- POST /auth/email/verify-request (auth)
- POST /auth/email/verify-confirm (public via token)

- POST /me/deactivate (auth)
- POST /me/delete (auth, soft delete)

---

## Me
- GET /me (auth)
- PATCH /me (auth)
- GET /me/bookings (auth)
- GET /me/inquiries (auth)
- GET /me/favorites (auth)
- GET /me/notifications (auth)
- POST /me/notifications/{notificationId}/read (auth)
- POST /me/notifications/read-all (auth)

---

## Organizations & Members
- POST /operator/organizations (auth) → creator becomes ORG_OWNER
- GET /organizations/{orgId} (public)
- GET /operator/organizations/{orgId} (ORG_MEMBER+)
- PATCH /operator/organizations/{orgId} (ORG_ADMIN/OWNER)
- GET /operator/organizations/{orgId}/members (ORG_ADMIN/OWNER)
- POST /operator/organizations/{orgId}/members/invitations (ORG_ADMIN/OWNER)
- POST /organizations/{orgId}/memberships/accept (invited user)
- PATCH /operator/organizations/{orgId}/members/{userId}/role (ORG_ADMIN/OWNER, owner-only for OWNER assignment)
- PATCH /operator/organizations/{orgId}/members/{userId}/deactivate (ORG_ADMIN/OWNER, owner-only for owner membership)

---

## Instructor registrations
- POST /instructor-registrations (auth)
- GET /organizations/{orgId}/instructor-registrations (ORG_ADMIN/OWNER)
- POST /instructor-registrations/{registrationId}/approve (ORG_ADMIN/OWNER)
- POST /instructor-registrations/{registrationId}/reject (ORG_ADMIN/OWNER)

---

## Instructor profile
- GET /me/instructor-profile?organizationId={orgId} (auth, current runtime shape)
- POST /me/instructor-profile (auth)
- PATCH /me/instructor-profile (auth)
- GET /instructors/{instructorProfileId} (public)
- GET /instructors/{instructorProfileId}/rating-summary (public)

---

## Taxonomy / Meta
- (MVP deferred in current OpenAPI)

---

## Tours (Template)
Operator:
- POST /organizations/{orgId}/tours (ORG_ADMIN/OWNER)
- GET /organizations/{orgId}/tours (ORG_ADMIN/OWNER)
- PATCH /tours/{tourId} (ORG_ADMIN/OWNER)
- POST /tours/{tourId}/publish (ORG_ADMIN/OWNER)
- PUT /tours/{tourId}/instructors (ORG_ADMIN/OWNER)

Public:
- GET /tours (public)
- GET /tours/{tourId} (public if PUBLISHED)
- GET /tours/{tourId}/instructors (public)
- GET /tours/{tourId}/rating-summary (public)

Content:
- GET /tours/{tourId}/content (public if PUBLISHED)
- PUT /tours/{tourId}/content (ORG_ADMIN/OWNER)

Assets:
- POST /assets/uploads (auth)
- POST /assets/{assetId}/complete (auth)
- PUT /operator/organizations/{orgId}/assets (ORG_ADMIN/OWNER)
- PUT /tours/{tourId}/assets (ORG_ADMIN/OWNER)

---

## Occurrences
Operator:
- POST /tours/{tourId}/occurrences (ORG_ADMIN/OWNER)
- PATCH /occurrences/{occurrenceId} (ORG_ADMIN/OWNER)
- POST /occurrences/{occurrenceId}/cancel (ORG_ADMIN/OWNER)
- POST /occurrences/{occurrenceId}/finish (ORG_ADMIN/OWNER)
- POST /occurrences/{occurrenceId}/reschedule (ORG_ADMIN/OWNER)
- POST /occurrences/{occurrenceId}/notify (ORG_ADMIN/OWNER)

Public:
- GET /tours/{tourId}/occurrences
- GET /occurrences/{occurrenceId}

Availability/Quote:
- GET /occurrences/{occurrenceId}/availability
- GET /occurrences/{occurrenceId}/quote

Current runtime note:
- Sprint 11 current runtime supports `locationText`, `meetingPoint`, `unitPrice`, and `currency` on occurrences.
- Current public search supports `locationText`, `dateFrom`, `dateTo`, `timezone`, `partySize`, `onlyAvailable`, `sort`, `cursor`, `limit`.
- Sprint 12 current runtime supports asset upload/complete plus organization/tour attachment replacement by ordered `assetIds`.
- Sprint 12 current runtime supports `GET /me/bookings`, calendar ICS export, favorites, and notifications read model APIs.

---

## Search
- GET /search/occurrences (public)
- POST /tours/{tourId}/favorite (auth)
- DELETE /tours/{tourId}/favorite (auth)

---

## Bookings

Create:
- POST /occurrences/{occurrenceId}/bookings (auth)

Read/List:
- GET /bookings/{bookingId} (participant or operator)
- GET /me/bookings (auth)

Operator actions:
- POST /bookings/{bookingId}/approve (ORG_ADMIN/OWNER)
- POST /bookings/{bookingId}/reject (ORG_ADMIN/OWNER)
- POST /bookings/{bookingId}/cancel (ORG_ADMIN/OWNER)
- POST /bookings/{bookingId}/complete (ORG_ADMIN/OWNER)
- PUT  /bookings/{bookingId}/attendance (ORG_ADMIN/OWNER)
- POST /bookings/{bookingId}/extend-offer (ORG_ADMIN/OWNER)
- POST /bookings/{bookingId}/force-expire (ORG_ADMIN/OWNER)

User actions:
- POST /bookings/{bookingId}/cancel (leader)
- PATCH /bookings/{bookingId}/party-size (leader decrease only)
- POST /bookings/{bookingId}/offer/accept (leader)
- POST /bookings/{bookingId}/offer/decline (leader)

Additional:
- GET /bookings/{bookingId}/explain (participant or operator)
- GET /bookings/{bookingId}/calendar.ics (accepted participant or leader)
- GET /occurrences/{occurrenceId}/calendar.ics (public if published + scheduled)

Waitlist (operator):
- GET /occurrences/{occurrenceId}/waitlist (ORG_ADMIN/OWNER)
- POST /waitlist/{waitlistId}/promote (ORG_ADMIN/OWNER)

---

## Participants
- GET /bookings/{bookingId}/participants (participant or operator)
- POST /bookings/{bookingId}/participants (leader; CONFIRMED; start-6h)

Deep link:
- POST /participants/{participantId}/accept (invitee)
- POST /participants/{participantId}/decline (invitee)

---

## Inquiries
- POST /occurrences/{occurrenceId}/inquiries (participants; bookingId required in body)
- GET /inquiries/{inquiryId} (participants or operator)
- GET /inquiries/{inquiryId}/messages (participants or operator)
- POST /inquiries/{inquiryId}/messages (participants or operator)
- POST /inquiries/{inquiryId}/close (participants or operator)

Operator inbox:
- (MVP deferred in current OpenAPI)

---

## Reviews
Create:
- POST /occurrences/{occurrenceId}/reviews/tour
- POST /occurrences/{occurrenceId}/reviews/instructor

Public summary:
- GET /tours/{tourId}/rating-summary
- GET /instructors/{instructorProfileId}/reviews/summary

Operator/private:
- GET /reviews/{reviewId}
- GET /organizations/{orgId}/reviews
- POST /moderation/content/reviews/{reviewId}/hide

---

## Policies
- GET /occurrences/{occurrenceId}/policies
- PUT /occurrences/{occurrenceId}/policies

---

## Notes (CS)
- POST /occurrences/{occurrenceId}/notes

---

## Announcements
- GET /public/announcements
- POST /organizations/{orgId}/announcements
- PATCH /announcements/{announcementId}
- DELETE /announcements/{announcementId}

---

## Favorites
- POST /tours/{tourId}/favorite
- DELETE /tours/{tourId}/favorite

---

## Calendar (ICS)
- GET /bookings/{bookingId}/calendar.ics
- GET /occurrences/{occurrenceId}/calendar.ics

---

## Reports
- GET /organizations/{orgId}/reports/bookings
- GET /organizations/{orgId}/reports/occurrences

---

## Moderation / User reports
- POST /moderation/users/{userId}/suspend
- POST /moderation/users/{userId}/unsuspend
