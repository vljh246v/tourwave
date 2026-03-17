# API Status Matrix

기준:
- 현재 코드 구현 상태
- `03_api_catalog.md`
- `04_openapi.yaml`
- Sprint 6 종료 시점 테스트 커버

이 문서는 "실제 구현된 API"와 "문서상 목표 API"의 차이를 빠르게 확인하기 위한 현재 상태 문서다.

## 1. Implemented and Stable

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
- `POST /bookings/{bookingId}/refund-preview`
- `POST /occurrences/{occurrenceId}/waitlist/promote`
- `POST /occurrences/{occurrenceId}/waitlist/skip`

### Participant / Attendance

- `POST /bookings/{bookingId}/participants/invitations`
- `POST /bookings/{bookingId}/participants/invitations/{participantId}/accept`
- `POST /bookings/{bookingId}/participants/invitations/{participantId}/decline`
- `POST /bookings/{bookingId}/participants/{participantId}/attendance`
- `GET /bookings/{bookingId}/participants`
- `GET /occurrences/{occurrenceId}/participants/roster`

### Inquiry

- `POST /occurrences/{occurrenceId}/inquiries`
- `POST /inquiries/{inquiryId}/messages`
- `POST /inquiries/{inquiryId}/close`
- `GET /inquiries/{inquiryId}`
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

## 2. Implemented But Not Fully Reflected In OpenAPI

- participant invitation path는 booking-scoped path를 기준으로 유지한다.
- attendance는 booking-level update가 아니라 participant-level path가 기준이다.
- review summary의 현재 기준 path는 `occurrenceId`다.
- refund preview는 booking 하위 리소스로 구현되어 있다.
- waitlist manual operation API는 code-first로 구현되어 있으나 OpenAPI 보강이 더 필요하다.

## 3. Planned / Blocked By Missing Domain Surface

다음 항목은 제품 비전에는 포함되지만 현재 코드에는 아직 없다.

- `tourId` / `instructorProfileId` / `organizationId` 기반 공개 review summary
- calendar export
- asset upload / complete / attach 흐름 전체
- favorites / announcements / report export
- organization/member management full CRUD
- auth/account/me 실제 JWT 플로우
- external payment callback/webhook intake

## 4. API Contract Handling Rule

- 새로운 개발은 무조건 이 문서와 실제 controller를 먼저 본다.
- `04_openapi.yaml`은 목표 계약 문서다.
- 구현이 먼저 있고 OpenAPI가 뒤처진 경우, 구현을 기준으로 이 문서를 먼저 맞춘 뒤 OpenAPI를 업데이트한다.
- 구현과 OpenAPI가 충돌하면 충돌 사실을 문서에 남기고 바로 정리한다. 방치하지 않는다.

## 5. Contract Test Anchors

- [BookingControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/domain/booking/application/BookingControllerIntegrationTest.kt)
- [MysqlPersistenceIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/MysqlPersistenceIntegrationTest.kt)
- [MysqlBookingConcurrencyTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/booking/MysqlBookingConcurrencyTest.kt)
