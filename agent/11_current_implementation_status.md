# Current Implementation Status

이 문서는 "지금 Tourwave 코드에 무엇이 구현되어 있고, 무엇이 아직 남았는가"를 handoff 용도로 정리한 문서다.

## 1. What Exists Today

현재 저장소는 예약 핵심 엔진, 계정 인증, 그리고 organization 운영 기초가 구현된 상태다. 비즈니스적으로는 "투어 authoring과 외부 결제가 빠진 예약 플랫폼 코어 + 운영 조직 관리 기초"로 보는 것이 정확하다.

### Product Grade Areas Already Implemented

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

- JWT access token runtime auth
- local/test header auth fallback
- role enum / organization scoped access checks
- organization persistence and operator/public profile split
- organization membership invite / accept / role change / deactivate
- `/me` membership projection
- minimal tour / instructor topology

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

## 2. What Is Not Product-Ready Yet

아래 항목은 실제 판매/운영 제품으로 가기 위해 필수인데 현재는 없거나 얇다.

### Product Surface Missing

- email verification / password reset
- me notification / favorite flow
- instructor registration/profile management full flow
- tour/occurrence authoring and publish/search APIs
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

## 3. Current Runtime Truths

- 프로덕션 기준 DB는 MySQL이다.
- 현재 저장소는 단일 Gradle 모듈이다.
- 실행 진입점은 API와 worker로 분리되어 있다.
- `mysql-test`는 현재 환경에서 H2 MySQL compatibility mode로 테스트된다.
- 인증은 JWT access token 기준이며, local/test 런타임만 request header fallback을 허용한다.
- organization은 persistence와 operator/public API가 올라왔지만, 투어/강사 authoring은 아직 얇다.

## 4. Current Code Structure Snapshot

- `domain`
  - booking, participant, payment, inquiry, review, occurrence, organization, instructor, tour, user
- `application`
  - booking, participant, inquiry, review, user, topology, common
- `adapter.in.web`
  - auth, booking, inquiry, organization, participant, review
- `adapter.in.job`
  - offer expiration, invitation expiration, refund retry, idempotency purge
- `adapter.out.persistence`
  - in-memory adapters
- `adapter.out.persistence.jpa`
  - MySQL/JPA adapters
- `bootstrap`
  - `UseCaseConfig`, `MysqlPersistenceConfig`, `ClockConfig`

## 5. Main Risks To Remember

- `04_openapi.yaml`은 구현보다 앞서 있거나 일부 경로가 다를 수 있다.
- 배치 작업은 구현되어 있지만 실제 다중 인스턴스 운영용 분산락은 아직 약하다.
- auth/account 영역은 아직 제품 표면 대비 비어 있다.
- true MySQL container 검증은 CI나 Docker 가능한 환경에서 다시 붙여야 한다.
- 외부 결제 승인/취소 이벤트를 수신하는 경로가 아직 없다.
- 조직/투어/강사 authoring이 없어 운영자가 실제 상품을 만들 수 없다.
- organization membership는 구현됐지만 초대 이메일 전달과 조직 전환 UX는 아직 없다.

## 6. If A New Agent Starts Today

가장 먼저 볼 곳:

1. `09_spec_index.md`
2. `12_runtime_topology_and_operations.md`
3. `13_api_status_matrix.md`
4. `16_product_delivery_roadmap.md`
5. `14_test_traceability_matrix.md`
6. 관련 controller / service / repository adapter 코드
