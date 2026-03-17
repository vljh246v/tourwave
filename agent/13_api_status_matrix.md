# API Status Matrix

기준:
- 현재 controller 구현 상태
- `03_api_catalog.md`의 목표 API
- `04_openapi.yaml`의 목표 계약
- Sprint 6 종료 시점 이후 코드 기준

이 문서는 "현재 실제로 호출 가능한 API"와 "제품 목표 API"의 차이를 빠르게 확인하기 위한 current truth 문서다.

## 1. Implemented and Stable Today

### Booking

- `POST /occurrences/{occurrenceId}/bookings`
- `GET /bookings/{bookingId}`
- `POST /bookings/{bookingId}/approve`
- `POST /bookings/{bookingId}/reject`
- `POST /bookings/{bookingId}/cancel`
- `POST /bookings/{bookingId}/offer/accept`
- `POST /bookings/{bookingId}/offer/decline`
- `PATCH /bookings/{bookingId}/party-size`
- `POST /bookings/{bookingId}/complete`
- `POST /occurrences/{occurrenceId}/cancel`
- `POST /occurrences/{occurrenceId}/finish`
- `GET /bookings/{bookingId}/refund-preview`
- `POST /bookings/{bookingId}/waitlist/promote`
- `POST /bookings/{bookingId}/waitlist/skip`

### Participant / Attendance

- `POST /bookings/{bookingId}/participants/invitations`
- `POST /bookings/{bookingId}/participants/invitations/{participantId}/accept`
- `POST /bookings/{bookingId}/participants/invitations/{participantId}/decline`
- `POST /bookings/{bookingId}/participants/{participantId}/attendance`
- `GET /bookings/{bookingId}/participants`
- `GET /occurrences/{occurrenceId}/participants/roster`
- `GET /occurrences/{occurrenceId}/participants/roster/export`

### Inquiry

- `POST /occurrences/{occurrenceId}/inquiries`
- `POST /inquiries/{inquiryId}/messages`
- `POST /inquiries/{inquiryId}/close`
- `GET /inquiries/{inquiryId}`
- `GET /inquiries/{inquiryId}/messages`
- `GET /me/inquiries`

### Review

- `POST /occurrences/{occurrenceId}/reviews/tour`
- `POST /occurrences/{occurrenceId}/reviews/instructor`
- `GET /occurrences/{occurrenceId}/reviews/summary`

### Runtime / Worker

- offer expiration job
- invitation expiration job
- refund retry job
- idempotency purge job

## 2. Important Current Runtime Notes

- 현재 인증은 JWT가 아니라 request header actor context다.
- participant invitation path는 booking-scoped path를 기준으로 유지한다.
- attendance는 booking-level update가 아니라 participant-level path가 기준이다.
- review summary의 현재 기준 path는 `occurrenceId`다.
- refund preview는 `GET /bookings/{bookingId}/refund-preview`로 구현되어 있다.
- waitlist manual operation API는 booking-scoped path로 구현되어 있다.

## 3. Not Implemented Yet But Needed For Product

다음 항목은 제품 비전에는 포함되지만 현재 코드에는 아직 없다.

- `POST /auth/signup`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`
- `GET /me`, `PATCH /me`, `GET /me/bookings`, `GET /me/favorites`, `GET /me/notifications`
- `POST /organizations`, `GET /organizations/{orgId}`, `GET/POST/PATCH /organizations/{orgId}/members`
- instructor registration/profile API 전반
- operator tour/occurrence authoring API 전반
- public tour/catalog/search/availability/quote API
- assets upload / complete / attach 흐름 전체
- `tourId` / `instructorProfileId` / `organizationId` 기반 공개 review summary
- calendar export
- favorites / announcements / report export
- external payment callback/webhook intake

## 4. API Contract Handling Rule

- 새로운 개발은 무조건 이 문서와 실제 controller를 먼저 본다.
- `04_openapi.yaml`은 목표 계약 문서다.
- 구현이 먼저 있고 OpenAPI가 뒤처진 경우, 구현을 기준으로 이 문서를 먼저 맞춘 뒤 OpenAPI를 업데이트한다.
- 구현과 OpenAPI가 충돌하면 충돌 사실을 문서에 남기고 바로 정리한다. 방치하지 않는다.

## 5. Contract Test Anchors

- [DocumentationBaselineTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/agent/DocumentationBaselineTest.kt)
- [BookingControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/domain/booking/application/BookingControllerIntegrationTest.kt)
- [MysqlPersistenceIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/MysqlPersistenceIntegrationTest.kt)
- [MysqlBookingConcurrencyTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/booking/MysqlBookingConcurrencyTest.kt)
