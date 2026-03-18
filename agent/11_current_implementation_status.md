# Current Implementation Status

이 문서는 "지금 Tourwave 코드에 무엇이 구현되어 있고, 무엇이 아직 남았는가"를 handoff 용도로 정리한 문서다.

## 1. What Exists Today

현재 저장소는 예약 핵심 엔진, 계정 인증, organization 운영 기초, instructor/tour authoring, occurrence public catalog/search, customer-facing asset/favorite/notification surface, 그리고 payment webhook/refund ops/reconciliation foundation이 구현된 상태다. 비즈니스적으로는 "판매 가능한 catalog와 기본 customer self-service, 그리고 운영자가 결제 실패와 정산 기초를 다룰 수 있는 예약 플랫폼 코어"로 보는 것이 정확하다.

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
- provider authorization / capture / refund boundary
- payment webhook persistence + signature verification
- refund ops queue / manual remediation
- reconciliation daily summary / export

### Authz / Topology

- JWT access token issuance and runtime resolution
- local/test header auth fallback
- role enum / organization scoped access checks
- organization persistence and operator/public profile split
- organization membership invite / accept / role change / deactivate
- instructor registration apply / approve / reject workflow
- instructor profile public/operator split
- operator tour CRUD / publish / structured content persistence
- operator occurrence create / update / reschedule
- public tours / public occurrences / availability / quote / search
- `/me` membership projection
- public tour content query
- asset upload / complete / attach workflow
- `GET /me/bookings`
- booking / occurrence calendar export
- favorites add / remove / list
- notifications read model / read / read-all

### Worker / Ops

- offer expiration job
- invitation expiration job
- refund retry job
- finance reconciliation daily summary job
- idempotency TTL purge job
- distributed lock coordinated job execution
- actuator health / liveness / readiness components
- actuator metrics surface for job execution
- structured audit payload support
- timezone-aware time window policy

### Persistence / Infra

- JPA adapter for booking, occurrence, participant, inquiry, review, user, payment, idempotency
- Flyway migration bootstrap
- MySQL runtime profile
- mysql-test profile
- worker job lock persistence
- real MySQL container smoke coverage
- occurrence lock 기반 capacity guard

## 2. What Is Not Product-Ready Yet

아래 항목은 실제 판매/운영 제품으로 가기 위해 필수인데 현재는 없거나 얇다.

### Product Surface Missing

- announcements / moderation / report APIs
- public review summary by tour / instructor / organization
- real outbound notification delivery
- real asset storage adapter
- real payment provider adapter

### Infra / Ops Hardening Missing

- alert routing / SLO dashboarding
- dead-letter style operator queue
- Gradle true multi-module split

### Recently Closed Gaps

- perimeter security enforcement in Spring Security filter chain
- email verification / password reset / account deactivation lifecycle

## 3. Current Runtime Truths

- 프로덕션 기준 DB는 MySQL이다.
- 현재 저장소는 단일 Gradle 모듈이다.
- 실행 진입점은 API와 worker로 분리되어 있다.
- `mysql-test`는 현재 환경에서 H2 MySQL compatibility mode로 테스트된다.
- CI는 별도 real MySQL container smoke test를 추가로 실행하도록 설계됐다.
- 인증 토큰은 JWT access token 기준이며, local/test 런타임만 request header fallback을 허용한다.
- 다만 현재 Spring Security filter chain이 경로 단위로 인증을 강제하는 구조는 아니고, 다수 endpoint가 controller/service guard에 의존한다.
- organization, instructor, tour, occurrence까지 운영자 authoring이 가능하고, public catalog/search 및 customer self-service 일부가 구현됐다.
- scheduled job은 distributed lock과 execution metric을 거쳐 실행된다.

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
- 배치 작업은 distributed lock까지 올라왔지만 alert routing과 stale lock 운영 기준은 계속 다듬어야 한다.
- auth/account 영역은 아직 제품 표면 대비 비어 있다.
- `SecurityConfig`는 현재 `anyRequest().permitAll()` 구성이므로 perimeter security hardening이 아직 끝나지 않았다.
- true MySQL container 검증은 smoke 수준까지 올라왔고 더 넓은 suite 확장이 남아 있다.
- 외부 결제 이벤트는 수신하지만 실제 third-party provider adapter와 webhook secret 운영 절차는 아직 stub 수준이다.
- asset upload는 fake storage URL issuance를 사용한다.
- notifications는 read model 조회까지는 구현됐지만 외부 email/SMS/push 발송은 없다.
- 운영자가 organization, instructor, tour, occurrence와 attachment를 다루고 고객은 booking/favorite/notification self-service가 가능하며 payment ops queue와 reconciliation까지 조회할 수 있지만, observability alerting과 실PG 연동이 아직 부족하다.
- organization membership는 구현됐지만 초대 이메일 전달과 조직 전환 UX는 아직 없다.

## 6. If A New Agent Starts Today

가장 먼저 볼 곳:

1. `09_spec_index.md`
2. `12_runtime_topology_and_operations.md`
3. `13_api_status_matrix.md`
4. `16_product_delivery_roadmap.md`
5. `17_release_gap_execution_plan.md`
6. `14_test_traceability_matrix.md`
7. 관련 controller / service / repository adapter 코드
