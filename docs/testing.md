# Testing

Layer discipline, scenario traceability, and drift guard contracts for the Tour Booking Platform.

> Run commands: see [README.md](../README.md) for basic commands; this file covers layer rules and scenario mapping.

---

## 1. Test Layer Discipline

The project follows hexagonal architecture. Each layer has distinct test responsibilities.

### Domain layer — `src/test/.../domain/`

Unit tests only. No Spring context, no DB, no infrastructure mocks.

- Test domain invariants, state machine transitions, eligibility rules, and business rule enforcement.
- Must not import Spring, JPA, or Testcontainers.

Key files:

| Test Class | What it covers |
|---|---|
| `domain/booking/BookingTest` | Booking aggregate invariants, status transitions |
| `domain/booking/BookingRefundPolicyTest` | Refund decision matrix (all boundary cases) |
| `domain/participant/BookingParticipantTest` | Participant aggregate invariants |
| `domain/service/UserCommandServiceTest` | User command domain rules |

### Application layer — `src/test/.../application/`

Unit tests with in-memory fakes (`support/FakeRepositories`). No Spring context, no real DB.

- Test use-case flows, service orchestration, and error handling.
- Infrastructure ports are faked; no mocking of domain objects.

Key areas: booking (approval, offers, refund retry, concurrency), participant (invitation, expiration), inquiry, review, payment webhooks, scheduling/jobs, and cross-cutting concerns (idempotency, time window policy).

### Adapter-in layer — `src/test/.../adapter/in/`

Integration tests with a real MySQL container (Testcontainers). Spring context loaded.

- Controller tests validate HTTP contract adherence, authz enforcement, and full response shapes.
- Job adapter tests verify scheduler delegation through `ScheduledJobCoordinator`.

Key files:

| Test Class | What it covers |
|---|---|
| `adapter/in/web/auth/AuthControllerIntegrationTest` | Auth flow, JWT, session lifecycle |
| `adapter/in/web/auth/RealModeSecurityIntegrationTest` | Security config in `real` profile |
| `adapter/in/web/organization/OrganizationControllerIntegrationTest` | Org create/update/membership authz |
| `adapter/in/web/topology/InstructorAndTourControllerIntegrationTest` | Instructor + tour CRUD |
| `adapter/in/web/customer/CustomerControllerIntegrationTest` | Me, bookings, favorites, notifications |
| `adapter/in/web/review/ReviewControllerIntegrationTest` | Review create, aggregation, access control |
| `adapter/in/web/payment/PaymentControllerIntegrationTest` | Webhook, reconciliation, remediation |
| `adapter/in/web/health/OperationalActuatorIntegrationTest` | Actuator health/metrics surface |
| `domain/booking/application/BookingControllerIntegrationTest` | Full booking lifecycle (approve/reject/cancel/offer) |
| `adapter/in/job/*JobTest` | Job delegation via coordinator |

### Adapter-out layer — `src/test/.../adapter/out/`

- `MysqlPersistenceIntegrationTest` — JPA/MySQL round-trip for all aggregate repositories.
- `RealMysqlContainerSmokeTest`, `RealMysqlContainerRegressionTest` — MySQL container boot smoke.
- `HttpPaymentProviderAdapterTest` — Payment gateway adapter (webhook signature, replay safety).
- `InMemoryTopologyRepositoryAdaptersTest` — In-memory topology adapters.

---

## 2. Mutation Test Rules (Idempotency / Audit / Error Contract)

All write endpoints that mutate booking/inquiry state must test:

| Rule | What to verify |
|---|---|
| Idempotency | Same `Idempotency-Key` + same payload → identical response, no duplicate row |
| Key reuse conflict | Same key + different payload → `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD` |
| State guard | Terminal-state transition attempt → `INVALID_BOOKING_STATE`, no partial mutation |
| Concurrent lock | Concurrent capture/refund on same booking → exactly one succeeds |
| Refund compensation | Refund API failure → `REFUND_PENDING`; retry job picks it up |
| Audit trail | Each domain action produces exactly one audit event with actor/action/resource/occurred_at/request_id |
| No double side-effect | Duplicate webhook processed once; dedupe by `provider_event_id` |

Reference: `docs/policies.md §4.3` (idempotency), `§4.6` (audit log), `§4.2` (payment compensation).

---

## 3. Scenario Traceability Matrix

Scenarios from the original test design → test class mapping.

Path prefix: `src/test/kotlin/com/demo/tourwave/`

| # | Scenario | Primary Test Class(es) | Notes |
|---|---|---|---|
| 1 | Basic booking approval (REQUESTED → CONFIRMED) | `domain/booking/application/BookingControllerIntegrationTest` | Approve + payment capture |
| 2 | Booking rejection (REQUESTED → REJECTED + refund) | `domain/booking/application/BookingControllerIntegrationTest` | |
| 3 | Waitlist creation when capacity is full | `domain/booking/application/BookingControllerIntegrationTest` | |
| 4 | Waitlist promotion — FIFO + fit, skip larger party | `application/booking/WaitlistOperatorServiceTest` | |
| 5 | Offer acceptance (OFFERED → CONFIRMED) | `domain/booking/application/BookingControllerIntegrationTest` | |
| 6 | Offer expiration by background job | `adapter/in/job/OfferExpirationJobTest` · `application/booking/OfferExpirationServiceTest` | |
| 7 | Participant invitation + accept | `application/participant/ParticipantCommandServiceTest` | |
| 8 | Invitation expiration | `application/participant/InvitedParticipantExpirationServiceTest` · `adapter/in/job/InvitationExpirationJobTest` | |
| 9 | Party size reduction → seats released | `domain/booking/application/BookingControllerIntegrationTest` | `PARTY_SIZE_INCREASE_NOT_ALLOWED` on increase |
| 10 | Inquiry messaging — leader ↔ operator ↔ participant | `application/inquiry/InquiryCommandServiceTest` · `application/inquiry/InquiryQueryServiceTest` | |
| 11 | Review eligibility — attendance = ATTENDED, occurrence finished | `application/review/ReviewQueryServiceTest` · `adapter/in/web/review/ReviewControllerIntegrationTest` | |
| 12 | Occurrence cancellation → all bookings CANCELED + refund | `application/topology/OccurrenceCommandServiceTest` | |
| 13 | Participant roster export (CSV) | `application/participant/ParticipantRosterQueryServiceTest` | |
| 14 | Invalid state transition rejected | `domain/booking/BookingTest` · `domain/booking/application/BookingControllerIntegrationTest` | `INVALID_BOOKING_STATE` |
| 15 | Offer accept after expiration rejected | `application/booking/OfferExpirationServiceTest` | `OFFER_EXPIRED` |
| 16 | Occurrence cancel stops all non-terminal bookings | `application/topology/OccurrenceCommandServiceTest` | |
| 17 | Inquiry scope validation — wrong occurrence/org | `application/inquiry/InquiryCommandServiceTest` | `BOOKING_SCOPE_MISMATCH` |
| 18 | Booking status decision: REQUESTED vs WAITLISTED | `domain/booking/application/BookingControllerIntegrationTest` | Capacity branch decision |
| 19 | Idempotent booking create — same payload | `domain/booking/application/BookingControllerIntegrationTest` | No duplicate row |
| 20 | Idempotency key reused with different payload | `domain/booking/application/BookingControllerIntegrationTest` | `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD` |
| 21 | Leader cancellation refund boundary (48h) | `domain/booking/BookingRefundPolicyTest` · `application/booking/BookingRefundPreviewServiceTest` | Both ≥48h and <48h cases |
| 22 | Refund API failure → REFUND_PENDING + retry job | `application/booking/RefundRetryServiceTest` · `adapter/in/job/RefundRetryJobTest` | |
| 23 | Invitation window boundary (6h, inclusive) | `application/common/TimeWindowPolicyServiceTest` · `application/participant/ParticipantCommandServiceTest` | `INVITE_WINDOW_CLOSED` |
| 24 | Offer expiration boundary (exact millisecond) | `application/common/TimeWindowPolicyServiceTest` · `application/booking/OfferExpirationServiceTest` | `OFFER_EXPIRED` |
| 25 | DST boundary for invite rule | `application/common/TimeWindowPolicyServiceTest` | Local-time policy, not naive UTC subtraction |
| 26 | Waitlist starvation mitigation — skip_count ≥ 3 | `application/booking/WaitlistOperatorServiceTest` | Priority boost after repeated skips |
| 27 | Concurrent accept vs expire race | `application/booking/MysqlBookingConcurrencyTest` | Row-level lock; exactly one terminal outcome |
| 28 | Occurrence cancel stops promotion pipeline | `application/topology/OccurrenceCommandServiceTest` | No new OFFEREDs after cancel |
| 29 | Audit event completeness | `domain/booking/application/BookingControllerIntegrationTest` | Partially covered; no dedicated audit test — see §5 |
| 30 | Inquiry idempotent create | `application/inquiry/InquiryCommandServiceTest` | |

---

## 4. Contract / Drift Guards

### DocumentationBaselineTest

`src/test/kotlin/com/demo/tourwave/agent/DocumentationBaselineTest.kt`

Asserts that 57 controller endpoint mappings (`@GetMapping`, `@PostMapping`, etc.) match the declared paths in `docs/architecture.md`. Fails CI if a controller is renamed or a path changes without updating the architecture doc. This is the primary drift guard between live code and documentation.

### OpenApiContractVerificationTest

`src/test/kotlin/com/demo/tourwave/agent/OpenApiContractVerificationTest.kt`

Parses `docs/openapi.yaml` and verifies structural contract rules: all operations have summaries, error responses include a `code` field, and response schemas follow naming conventions. Fails fast on malformed YAML or schema violations before any runtime test is executed.

---

## 5. Coverage Gaps

| Gap | Status |
|---|---|
| Audit event completeness (Scenario 29) | No dedicated `AuditEventTest`; partially covered by booking flow integration tests |
| `CommunicationReportingIntegrationTest` | Pre-existing failure on `main`; not caused by this branch |
| `OccurrenceCatalogControllerIntegrationTest` | Pre-existing failure on `main`; not caused by this branch |
| Launch alert automation | Threshold/runbook documented in `docs/operations.md`; Pager/Slack wiring is infra work pending env setup |
| Gradle multi-module split | Runtime entry-point separation done; build module split not yet implemented |

---

## 6. Running Tests

| Command | Purpose |
|---|---|
| `./gradlew test` | Full suite (requires Docker for Testcontainers MySQL) |
| `./gradlew test --tests "*.BookingControllerIntegrationTest"` | Full booking lifecycle |
| `./gradlew test --tests "*.BookingRefundPolicyTest"` | Refund policy domain unit (no DB) |
| `./gradlew test --tests "*.TimeWindowPolicyServiceTest"` | Boundary and DST scenarios |
| `./gradlew test --tests "*.MysqlBookingConcurrencyTest"` | Concurrency tests (slower, real MySQL) |
| `./gradlew test --tests "*.OpenApiContractVerificationTest"` | OpenAPI contract guard only |
| `./gradlew test --tests "*.DocumentationBaselineTest"` | Endpoint drift guard only |

---

## Cross-References

| Document | Content |
|---|---|
| `docs/domain-rules.md` | Invariants and state machines that tests must protect |
| `docs/policies.md §4` | Operational policy tables (refund, idempotency, time, audit) |
| `docs/openapi.yaml` | HTTP contract verified by `OpenApiContractVerificationTest` |
| `docs/architecture.md` | Hexagonal layer rules enforced by `DocumentationBaselineTest` |
