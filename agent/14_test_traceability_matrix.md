# Test Traceability Matrix

이 문서는 현재 구현이 어떤 테스트로 보호되는지, 그리고 어디가 아직 빈약한지를 handoff 관점에서 정리한다.

## 1. Booking / Capacity

- booking create requested/waitlisted
  - [BookingControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/domain/booking/application/BookingControllerIntegrationTest.kt)
- idempotent booking mutation
  - [BookingControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/domain/booking/application/BookingControllerIntegrationTest.kt)
- refund policy decision matrix
  - [BookingRefundPolicyTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/domain/booking/BookingRefundPolicyTest.kt)
- refund preview
  - [BookingRefundPreviewServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/booking/BookingRefundPreviewServiceTest.kt)
- concurrent approve capacity guard
  - [MysqlBookingConcurrencyTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/booking/MysqlBookingConcurrencyTest.kt)

## 2. Participant / Attendance

- participant aggregate invariants
  - [BookingParticipantTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/domain/participant/BookingParticipantTest.kt)
- invitation create, accept, decline
  - [ParticipantCommandServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/participant/ParticipantCommandServiceTest.kt)
- participant query / roster
  - [ParticipantQueryServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/participant/ParticipantQueryServiceTest.kt)
  - [ParticipantRosterQueryServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/participant/ParticipantRosterQueryServiceTest.kt)
- invitation expiration
  - [InvitedParticipantExpirationServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/participant/InvitedParticipantExpirationServiceTest.kt)

## 3. Inquiry

- inquiry create with initial message
  - [InquiryCommandServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/inquiry/InquiryCommandServiceTest.kt)
- inquiry detail/list/query
  - [InquiryQueryServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/inquiry/InquiryQueryServiceTest.kt)

## 4. Worker / Operational Jobs

- offer expiration job
  - [OfferExpirationJobTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/job/OfferExpirationJobTest.kt)
- invitation expiration job
  - [InvitationExpirationJobTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/job/InvitationExpirationJobTest.kt)
- refund retry
  - [RefundRetryServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/booking/RefundRetryServiceTest.kt)
- idempotency TTL purge
  - [IdempotencyPurgeServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/common/IdempotencyPurgeServiceTest.kt)

## 5. Persistence

- JPA/MySQL-compatible persistence round trip
  - [MysqlPersistenceIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/MysqlPersistenceIntegrationTest.kt)
- profile wiring / app boot
  - [ProfileConfigurationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/ProfileConfigurationTest.kt)
  - [TourwaveApplicationTests](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/TourwaveApplicationTests.kt)

## 6. Documentation / Contract Baseline

- current truth 문서 핵심 진술과 drift-prone endpoint baseline
  - [DocumentationBaselineTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/agent/DocumentationBaselineTest.kt)
- request header actor context parsing baseline
  - [RequestHeaderActorContextResolverTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/authz/RequestHeaderActorContextResolverTest.kt)
- JWT issue/parse and auth command flow
  - [JwtTokenServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/auth/JwtTokenServiceTest.kt)
  - [AuthCommandServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/auth/AuthCommandServiceTest.kt)
- auth/me integration and runtime fallback policy
  - [AuthControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/auth/AuthControllerIntegrationTest.kt)
  - [RealModeSecurityIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/auth/RealModeSecurityIntegrationTest.kt)
- organization create/update/membership lifecycle and operator authz
  - [OrganizationCommandServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/topology/OrganizationCommandServiceTest.kt)
  - [OrganizationControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/organization/OrganizationControllerIntegrationTest.kt)
- instructor registration apply/approve/reject and profile projection
  - [InstructorRegistrationServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/topology/InstructorRegistrationServiceTest.kt)
  - [InstructorAndTourControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/topology/InstructorAndTourControllerIntegrationTest.kt)
- operator tour CRUD, publish, and structured content query
  - [TourCommandServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/topology/TourCommandServiceTest.kt)
  - [InstructorAndTourControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/topology/InstructorAndTourControllerIntegrationTest.kt)
- occurrence authoring validation, availability/quote/search projection
  - [OccurrenceCommandServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/topology/OccurrenceCommandServiceTest.kt)
  - [CatalogQueryServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/topology/CatalogQueryServiceTest.kt)
  - [OccurrenceCatalogControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/topology/OccurrenceCatalogControllerIntegrationTest.kt)
- asset upload/complete/attach workflow
  - [AssetCommandServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/asset/AssetCommandServiceTest.kt)
  - [CustomerControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/customer/CustomerControllerIntegrationTest.kt)
- my bookings / calendar export
  - [CustomerBookingQueryServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/customer/CustomerBookingQueryServiceTest.kt)
  - [CustomerControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/customer/CustomerControllerIntegrationTest.kt)
- favorites query and mutation
  - [FavoriteServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/customer/FavoriteServiceTest.kt)
  - [CustomerControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/customer/CustomerControllerIntegrationTest.kt)
- notifications read model and read state transitions
  - [NotificationServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/customer/NotificationServiceTest.kt)
  - [CustomerControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/customer/CustomerControllerIntegrationTest.kt)

## 7. Known Coverage Gaps

- real MySQL container execution
  - 현재 환경에는 Docker provider가 없어서 `mysql-test`는 H2 MySQL compatibility mode로 검증한다.
- OpenAPI parser 기반 contract validation
  - smoke/integration coverage는 있으나 spec parser 기반 자동 검증은 아직 없다.
- richer review aggregation / operator reporting contract
  - asset attachment와 customer calendar/export surface는 Sprint 12에서 구현됐지만, review aggregation by tour/instructor/org and operator reporting은 아직 없다.
