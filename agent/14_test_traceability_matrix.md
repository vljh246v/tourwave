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
- distributed job lock, skip/failure metric, execution tracking
  - [ScheduledJobCoordinatorTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/common/ScheduledJobCoordinatorTest.kt)
- worker job adapters delegate through distributed coordinator
  - [OfferExpirationJobTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/job/OfferExpirationJobTest.kt)
  - [InvitationExpirationJobTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/job/InvitationExpirationJobTest.kt)
  - [RefundRetryJobTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/job/RefundRetryJobTest.kt)
  - [IdempotencyPurgeJobTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/job/IdempotencyPurgeJobTest.kt)
  - [FinanceReconciliationJobTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/job/FinanceReconciliationJobTest.kt)

## 5. Persistence

- JPA/MySQL-compatible persistence round trip
  - [MysqlPersistenceIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/MysqlPersistenceIntegrationTest.kt)
- real MySQL container smoke
  - [RealMysqlContainerSmokeTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/RealMysqlContainerSmokeTest.kt)
  - [RealMysqlContainerRegressionTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/out/persistence/jpa/RealMysqlContainerRegressionTest.kt)
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
- review aggregation by occurrence / tour / instructor / organization
  - [ReviewQueryServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/review/ReviewQueryServiceTest.kt)
  - [ReviewControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/review/ReviewControllerIntegrationTest.kt)
- payment webhook signature verification and replay-safe processing
  - [PaymentWebhookServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/payment/PaymentWebhookServiceTest.kt)
  - [PaymentControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/payment/PaymentControllerIntegrationTest.kt)
- refund ops queue and manual remediation
  - [RefundOperationsServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/payment/RefundOperationsServiceTest.kt)
  - [RefundRetryServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/booking/RefundRetryServiceTest.kt)
- cross-surface operator remediation queue
  - [OperatorRemediationQueueServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/operations/OperatorRemediationQueueServiceTest.kt)
  - [PaymentControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/payment/PaymentControllerIntegrationTest.kt)
- reconciliation daily summary and export
  - [ReconciliationServiceTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/application/payment/ReconciliationServiceTest.kt)
  - [PaymentControllerIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/payment/PaymentControllerIntegrationTest.kt)
- actuator health / metrics operational surface
  - [OperationalActuatorIntegrationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/adapter/in/web/health/OperationalActuatorIntegrationTest.kt)
- OpenAPI parser based contract verification
  - [OpenApiContractVerificationTest](/Users/jaehyeon/Documents/workspace/tourwave/src/test/kotlin/com/demo/tourwave/agent/OpenApiContractVerificationTest.kt)

## 7. Known Coverage Gaps

- launch alert automation
  - threshold/runbook 문서는 닫혔지만 실제 Pager/Slack wiring은 환경별 인프라 작업이 남아 있다.
- Gradle multi-module split
  - 런타임 분리는 되어 있지만 build module split은 아직 아니다.
