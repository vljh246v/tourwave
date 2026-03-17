# Sprint 6 Scenario Traceability Matrix

## Booking / Capacity

- booking create requested/waitlisted
  - [BookingControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/domain/booking/application/BookingControllerIntegrationTest.kt)
- idempotent booking mutation
  - [BookingControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/domain/booking/application/BookingControllerIntegrationTest.kt)
- concurrent approve capacity guard
  - [MysqlBookingConcurrencyTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/booking/MysqlBookingConcurrencyTest.kt)

## Participant / Attendance

- invitation create, accept, decline, expire
  - [BookingControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/domain/booking/application/BookingControllerIntegrationTest.kt)
  - [ParticipantCommandServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/participant/ParticipantCommandServiceTest.kt)
  - [InvitedParticipantExpirationServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/participant/InvitedParticipantExpirationServiceTest.kt)

## Inquiry

- inquiry create with initial message
  - [InquiryCommandServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/inquiry/InquiryCommandServiceTest.kt)
- inquiry detail/list/query
  - [InquiryQueryServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/inquiry/InquiryQueryServiceTest.kt)

## Refund / Payment

- refund policy decision matrix
  - [BookingRefundPolicyTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/domain/booking/BookingRefundPolicyTest.kt)
- refund preview
  - [BookingRefundPreviewServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/booking/BookingRefundPreviewServiceTest.kt)
- retryable refund reprocessing
  - [RefundRetryServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/booking/RefundRetryServiceTest.kt)

## Persistence / Idempotency

- JPA/MySQL-compatible persistence round trip
  - [MysqlPersistenceIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/MysqlPersistenceIntegrationTest.kt)
- idempotency TTL purge
  - [IdempotencyPurgeServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/common/IdempotencyPurgeServiceTest.kt)

## Worker Jobs

- offer expiration job
  - [OfferExpirationJobTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/job/OfferExpirationJobTest.kt)
- invitation expiration job
  - [InvitationExpirationJobTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/job/InvitationExpirationJobTest.kt)

## Remaining Gaps

- real MySQL container execution:
  - 현재 환경에는 Docker provider가 없어서 `mysql-test`는 H2 MySQL compatibility mode로 검증한다.
- public API contract validation against OpenAPI:
  - smoke/integration coverage는 있으나 OpenAPI parser 기반 자동 검증은 아직 없다.
