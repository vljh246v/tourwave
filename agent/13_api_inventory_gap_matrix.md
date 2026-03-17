# Sprint 6 API Inventory / Gap Matrix

기준:
- 코드 구현 상태
- `03_api_catalog.md`
- `04_openapi.yaml`
- Sprint 6 종료 시점 테스트 커버

## Implemented and Stable

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

### Participants
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

### Ops / Worker
- waitlist manual promote/skip
- offer expiration job
- invitation expiration job
- refund retry job
- idempotency purge job

## Needs OpenAPI Correction

- participant invitation path는 booking-scoped path를 기준으로 유지해야 한다.
- attendance는 booking-level 단건 update가 아니라 participant-level path가 맞다.
- review summary는 현재 `occurrenceId` 기준 구현이 기준이다.
- refund preview는 booking mutation 축에 포함돼야 한다.

## Planned / Blocked by Domain

- `tourId` / `instructorProfileId` / `organizationId` 기반 공개 review summary
- calendar export
- asset attachment public API
- favorites / announcements / report export
- org/member management full CRUD
- external payment callback/webhook intake

## Contract Test Anchors

- [BookingControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/domain/booking/application/BookingControllerIntegrationTest.kt)
- [MysqlPersistenceIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/MysqlPersistenceIntegrationTest.kt)
- [MysqlBookingConcurrencyTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/booking/MysqlBookingConcurrencyTest.kt)
