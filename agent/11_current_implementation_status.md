# Current Implementation Status

이 문서는 "지금 Tourwave 코드에 무엇이 구현되어 있고, 무엇이 아직 남았는가"를 handoff 용도로 정리한 문서다.

## 1. Completed Through Sprint 6

### Domain / Core Flow

- booking lifecycle
  - create, approve, reject, cancel, complete
- waitlist lifecycle
  - waitlisted, offered, offer accept, offer decline, offer expire
- participant lifecycle
  - leader participant, invitation create, accept, decline, expire
- attendance lifecycle
  - participant attendance mark
- review lifecycle
  - attendance 기반 eligibility

### Query / Operator Flow

- booking detail query
- inquiry detail / list query
- participant list / roster query
- waitlist operator manual promote / skip

### Refund / Payment

- refund policy calculator
- refund preview
- cancel flow refund integration
- payment ledger / payment status model
- refund retry flow

### Authz / Topology

- header 기반 actor context
- role enum / organization scoped access checks
- minimal organization / tour / instructor topology

### Worker / Ops

- offer expiration job
- invitation expiration job
- refund retry job
- idempotency TTL purge job
- structured audit payload support
- timezone-aware time window policy

### Persistence / Infra

- JPA adapter for booking, occurrence, participant, inquiry, review, user, payment, idempotency
- Flyway migration bootstrap
- MySQL runtime profile
- mysql-test profile
- occurrence lock 기반 capacity guard

## 2. What Is Still Missing

### Product Surface Missing

- auth signup/login/jwt/refresh
- me profile / notification / favorite flow
- organization/member management full CRUD
- instructor registration/profile management full flow
- tour/occurrence authoring APIs
- assets upload/complete/attach flow
- announcements / moderation / report APIs
- calendar export
- public review summary by tour / instructor / organization
- external payment webhook / callback

### Infra / Ops Hardening Missing

- real MySQL container-based test execution in current environment
- distributed lock strategy for multi-instance worker deployment
- OpenAPI parser 기반 contract verification
- metrics / alerting / dead-letter style operator queue
- Gradle true multi-module split

## 3. Current Code Structure Snapshot

- `domain`
  - booking, participant, payment, inquiry, review, occurrence, organization, instructor, tour, user
- `application`
  - booking, participant, inquiry, review, user, topology, common
- `adapter.in.web`
  - booking, inquiry, participant, review
- `adapter.in.job`
  - offer expiration, invitation expiration, refund retry, idempotency purge
- `adapter.out.persistence`
  - in-memory adapters
- `adapter.out.persistence.jpa`
  - MySQL/JPA adapters
- `bootstrap`
  - `UseCaseConfig`, `MysqlPersistenceConfig`, `ClockConfig`

## 4. Current Runtime Truths

- 프로덕션 기준 DB는 MySQL이다.
- 현재 저장소는 여전히 단일 Gradle 모듈이다.
- 다만 실행 진입점은 API와 worker로 분리되어 있다.
- `mysql-test`는 현재 환경에서는 H2 MySQL compatibility mode로 테스트된다.

## 5. Main Risks To Remember

- `04_openapi.yaml`은 구현보다 앞서 있거나 일부 경로가 다를 수 있다.
- 배치 작업은 구현되어 있지만 실제 다중 인스턴스 운영용 분산락은 아직 약하다.
- auth/account 영역은 아직 제품 표면 대비 비어 있다.
- true MySQL container 검증은 CI나 Docker 가능한 환경에서 다시 붙여야 한다.

## 6. If A New Agent Starts Today

가장 먼저 볼 곳:

1. `09_spec_index.md`
2. `12_runtime_topology_and_operations.md`
3. `13_api_status_matrix.md`
4. `14_test_traceability_matrix.md`
5. 관련 controller / service / repository adapter 코드
