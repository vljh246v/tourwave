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

### Auth / Account

- `POST /auth/signup`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /me`
- `PATCH /me`

### Organization / Operator

- `POST /operator/organizations`
- `GET /operator/organizations/{organizationId}`
- `PATCH /operator/organizations/{organizationId}`
- `GET /operator/organizations/{organizationId}/members`
- `POST /operator/organizations/{organizationId}/members/invitations`
- `PATCH /operator/organizations/{organizationId}/members/{memberUserId}/role`
- `PATCH /operator/organizations/{organizationId}/members/{memberUserId}/deactivate`
- `GET /organizations/{organizationId}`
- `POST /organizations/{organizationId}/memberships/accept`

### Instructor

- `POST /instructor-registrations`
- `GET /organizations/{organizationId}/instructor-registrations`
- `POST /instructor-registrations/{registrationId}/approve`
- `POST /instructor-registrations/{registrationId}/reject`
- `GET /me/instructor-profile?organizationId=...`
- `POST /me/instructor-profile`
- `PATCH /me/instructor-profile`
- `GET /instructors/{instructorProfileId}`

### Tour Authoring

- `POST /organizations/{organizationId}/tours`
- `GET /organizations/{organizationId}/tours`
- `PATCH /tours/{tourId}`
- `POST /tours/{tourId}/publish`
- `PUT /tours/{tourId}/content`
- `POST /tours/{tourId}/occurrences`
- `PATCH /occurrences/{occurrenceId}`
- `POST /occurrences/{occurrenceId}/reschedule`
- `PUT /operator/organizations/{organizationId}/assets`
- `PUT /tours/{tourId}/assets`

### Public Catalog / Search

- `GET /tours`
- `GET /tours/{tourId}`
- `GET /tours/{tourId}/content`
- `GET /tours/{tourId}/occurrences`
- `GET /occurrences/{occurrenceId}`
- `GET /occurrences/{occurrenceId}/availability`
- `GET /occurrences/{occurrenceId}/quote`
- `GET /search/occurrences`
- `GET /occurrences/{occurrenceId}/calendar.ics`

### Assets / Customer Surface

- `POST /assets/uploads`
- `POST /assets/{assetId}/complete`
- `GET /me/bookings`
- `GET /bookings/{bookingId}/calendar.ics`
- `POST /tours/{tourId}/favorite`
- `DELETE /tours/{tourId}/favorite`
- `GET /me/favorites`
- `GET /me/notifications`
- `POST /me/notifications/{notificationId}/read`
- `POST /me/notifications/read-all`

### Payment / Finance Operations

- `POST /payments/webhooks/provider`
- `GET /operator/payments/refunds/ops`
- `POST /operator/payments/bookings/{bookingId}/refund-retry`
- `GET /operator/finance/reconciliation/daily`
- `POST /operator/finance/reconciliation/daily/{summaryDate}/refresh`
- `GET /operator/finance/reconciliation/daily/export`

### Runtime / Ops Endpoints

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/metrics/tourwave.job.execution`

### Runtime / Worker

- offer expiration job
- invitation expiration job
- refund retry job
- idempotency purge job
- distributed lock coordinated execution
- finance reconciliation job

## 2. Important Current Runtime Notes

- 현재 인증은 JWT access token 기반이고, local/test 런타임에서는 request header actor context fallback이 허용된다.
- participant invitation path는 booking-scoped path를 기준으로 유지한다.
- attendance는 booking-level update가 아니라 participant-level path가 기준이다.
- review summary의 현재 기준 path는 `occurrenceId`다.
- refund preview는 `GET /bookings/{bookingId}/refund-preview`로 구현되어 있다.
- waitlist manual operation API는 booking-scoped path로 구현되어 있다.
- `real` 런타임에서는 header fallback 없이 Bearer JWT가 필요하다.
- `/me`는 현재 사용자 profile과 organization memberships를 함께 반환한다.
- organization operator API는 `/operator/organizations/...` 네임스페이스를 사용하고, 공개 조회는 `/organizations/{organizationId}`에 분리되어 있다.
- instructor registration 승인 시 instructor profile이 자동 생성되며, 이후 `me` profile API로 수정한다.
- 현재 instructor `me` 조회는 단일 path를 유지하기 위해 `organizationId` query parameter를 사용한다.
- public catalog는 published tour + scheduled occurrence만 노출한다.
- occurrence create/update는 가격 변경 없이 authoring 필드만 수정하고, reschedule은 시간/장소 변경 전용 path로 분리되어 있다.
- search는 현재 `locationText`, `dateFrom/dateTo`, `timezone`, `partySize`, `onlyAvailable`, `sort`, `cursor`, `limit`를 지원한다.
- quote 응답은 가격 계산과 함께 현재 환불 설명 문자열 및 full refund deadline을 포함한다.
- asset upload는 fake storage URL issuance를 사용하고, `READY` 상태 asset만 organization/tour에 ordered attach할 수 있다.
- `GET /me/bookings`는 leader booking과 accepted participant booking을 함께 보여준다.
- booking calendar export는 participant access policy를 그대로 따르고, occurrence calendar export는 published + scheduled occurrence에 한해 공개된다.
- favorites는 published tour만 허용한다.
- notifications는 booking/inquiry/refund 관련 audit event에서 read model로 축적되고, `read`/`read-all` API를 지원한다.
- payment webhook intake는 `X-Payment-Signature` HMAC 검증을 수행하고, `providerEventId` 기준 replay-safe 처리로 중복 이벤트를 무시한다.
- refund ops queue는 `REFUND_PENDING`, `REFUND_FAILED_RETRYABLE`, `REFUND_REVIEW_REQUIRED` 상태를 운영자가 조회하고 booking 단위 manual retry를 수행할 수 있다.
- reconciliation daily summary는 booking 생성 건수와 payment ledger status 업데이트 건수를 일자별로 저장하고 JSON/CSV 조회를 지원한다.
- actuator health는 `workerJobs`, `workerJobLocks`, liveness, readiness component를 노출한다.
- scheduled job은 distributed lock을 선점한 인스턴스만 실행하고, 나머지 인스턴스는 skip metric만 남긴다.
- execution metric은 `tourwave.job.execution`, `tourwave.job.execution.duration`, `tourwave.job.lock.skipped`로 기록된다.

## 3. Not Implemented Yet But Needed For Product

다음 항목은 제품 비전에는 포함되지만 현재 코드에는 아직 없다.

- `tourId` / `instructorProfileId` / `organizationId` 기반 공개 review summary
- favorites / announcements / report export

## 4. API Contract Handling Rule

- 새로운 개발은 무조건 이 문서와 실제 controller를 먼저 본다.
- `04_openapi.yaml`은 목표 계약 문서다.
- 구현이 먼저 있고 OpenAPI가 뒤처진 경우, 구현을 기준으로 이 문서를 먼저 맞춘 뒤 OpenAPI를 업데이트한다.
- 구현과 OpenAPI가 충돌하면 충돌 사실을 문서에 남기고 바로 정리한다. 방치하지 않는다.

## 5. Contract Test Anchors

- [DocumentationBaselineTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/agent/DocumentationBaselineTest.kt)
- [BookingControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/domain/booking/application/BookingControllerIntegrationTest.kt)
- [OccurrenceCatalogControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/topology/OccurrenceCatalogControllerIntegrationTest.kt)
- [CustomerControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/customer/CustomerControllerIntegrationTest.kt)
- [PaymentControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/payment/PaymentControllerIntegrationTest.kt)
- [MysqlPersistenceIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/MysqlPersistenceIntegrationTest.kt)
- [MysqlBookingConcurrencyTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/booking/MysqlBookingConcurrencyTest.kt)
