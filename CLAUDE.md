# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 작업 진입점

| 작업 유형 | 먼저 읽을 문서 | 시작 명령 |
|-----------|---------------|-----------|
| 새 기능 / 버그 수정 | `docs/golden-principles.md` → `docs/escalation-policy.md` | `/harness-task <id> <설명>` |
| 고위험 변경 (DB 스키마, 인증, 상태 머신) | `docs/escalation-policy.md` → 사람 승인 필수 | - |
| 반복 실패 분석 | `logs/trends/failure-patterns.md` → `docs/agent-failures.md` | - |

## 하네스 워크플로우

```
/harness-task <task-id> <설명>
  → 계획 → task-start.sh → 구현 → verify-task.sh → PR push
```

수동 실행:
```bash
./scripts/task-start.sh <task-id>    # 워크트리 생성
./scripts/verify-task.sh <task-id>   # 검증만 (병합 안 함)
./scripts/task-finish.sh <task-id>   # push + PR 안내
```

핵심 문서:
- `docs/golden-principles.md` — 반복 실패에서 승격된 누적 규칙
- `docs/escalation-policy.md` — 사람 승인 필수 변경 목록
- `docs/agent-failures.md` — 에이전트 실패 로그
- `logs/trends/failure-patterns.md` — 최근 실패 패턴 (Feedforward)

## 프로젝트 개요

Tourwave는 투어/액티비티 운영사를 위한 예약 플랫폼 백엔드다. 고객 예약, 운영 실행, 사후 관리(출석/리뷰/환불)를 하나의 도메인으로 다룬다. Spring Boot 3.3.1 / Kotlin 1.9.24 / JDK 17, MySQL + Flyway, Spring Security (JWT), Micrometer + Prometheus.

## 빌드 & 테스트 명령어

```bash
# 전체 테스트 (Testcontainers 사용 - Docker 필요)
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "*.BookingControllerIntegrationTest"
./gradlew test --tests "*.BookingRefundPolicyTest"

# 주요 개별 스위트
./gradlew test --tests "com.demo.tourwave.agent.OpenApiContractVerificationTest"   # OpenAPI 계약 가드
./gradlew test --tests "com.demo.tourwave.agent.DocumentationBaselineTest"          # 엔드포인트 drift 가드
./gradlew test --tests "com.demo.tourwave.adapter.out.persistence.jpa.RealMysqlContainerRegressionTest"
./gradlew test --tests "*.MysqlBookingConcurrencyTest"                              # 행 수준 락 테스트
```

## 아키텍처: 헥사고날 레이어

패키지 루트: `com.demo.tourwave`

| 레이어 | 패키지 | 규칙 |
|---|---|---|
| `domain` | `domain.<bounded-context>` | 엔티티, 값 객체, 상태전이. **Spring/JPA/Web import 금지.** |
| `application` | `application.<bounded-context>` | 유스케이스 조합, 트랜잭션 경계, 정책 호출. Port 인터페이스에만 의존. |
| `adapter.in` | `adapter.in.web.<bounded-context>` | HTTP 컨트롤러, 메시지 컨슈머. DTO ↔ Command 변환. 에러코드 매핑 담당. |
| `adapter.out` | `adapter.out.persistence.<bounded-context>` | JPA/MySQL 구현체, 외부 API 클라이언트. Port 구현 담당. |
| `bootstrap` | `bootstrap` | Bean 조립 (`MysqlPersistenceConfig`, `AuthConfig`, `UseCaseConfig` 등) |

**의존 방향은 단방향:** `adapter.in → application → domain`; `adapter.out → port`. 경계를 절대 역방향으로 넘지 않는다 (예: `application`에서 `adapter.out` 구체 클래스 import 금지).

**동일 Gradle 모듈에 두 개의 런타임 진입점이 존재:**
- `TourwaveApplication` — API 서버
- `WorkerApplication` (`com.demo.tourwaveworker`) — 백그라운드 잡 워커

워커 잡은 오케스트레이션 전용 — `application` 서비스를 호출하며, 도메인 로직을 직접 보유하지 않는다.

## 프로필 / 퍼시스턴스 전환

- 프로필 `mysql` / `mysql-test` → JPA 어댑터 활성화 (`MysqlPersistenceConfig`)
- 그 외 프로필 → in-memory 어댑터 활성화
- `mysql-test`는 현재 H2 MySQL 호환 모드 사용; 실제 MySQL 컨테이너 검증은 `RealMysqlContainer*` 테스트 클래스 사용

## 스펙 문서 (우선순위 순)

`docs/domain-rules.md` > `docs/architecture.md` > `docs/policies.md` > `docs/schema.md` > `docs/openapi.yaml`

충돌 시 domain-rules가 우선한다.

| 문서 | 용도 |
|---|---|
| `docs/domain-rules.md` | 예약 상태 머신, 정원 불변식, 아이덤포턴시, 감사 이벤트, 환불 규칙 |
| `docs/architecture.md` | 레이어 가드레일, PR 체크리스트, 리팩터 정책 |
| `docs/policies.md` | 인증/역할 매트릭스, 에러코드, 운영 정책표 (환불/아이덤포턴시/시간·타임존/대기열) |
| `docs/schema.md` | MySQL 스키마 규약 |
| `docs/openapi.yaml` | HTTP 계약 SSOT (API 형태 및 에러코드 맵) |
| `docs/testing.md` | 레이어별 테스트 규율, 시나리오 추적 매트릭스, 미커버 항목 |
| `docs/operations.md` | 런타임 토폴로지, 지표, 환경 매트릭스, 출시 체크리스트 |

## 핵심 도메인 규칙 (구현 필수)

- **모든 상태 변경 엔드포인트에 `Idempotency-Key` 헤더 필수** (예약 create/approve/reject/cancel, offer accept/decline, 문의 create/message/close). 동일 키 + 다른 payload → `422 IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD`.
- **감사 이벤트(append-only)** 는 예약 상태 변경, offer 생명주기, occurrence cancel/finish, 결제 액션마다 반드시 기록한다.
- **트랜잭션 경계는 `application`에서 관리**; 좌석/정원 흐름은 occurrence 행 락을 먼저 획득한다.
- **모든 타임스탬프는 UTC** (`*_at_utc` 컬럼). 시간 경계 규칙(초대 6h 차단, offer 만료, 초대 48h)은 occurrence의 IANA 타임존 기준으로 계산 — DB 타임존 함수 사용 금지, application 레이어에서만 변환.
- **터미널 예약 상태** (`REJECTED`, `EXPIRED`, `CANCELED`, `COMPLETED`) — 추가 전이 불가; 중복 커맨드는 아이덤포턴트 no-op으로 처리.
- **불변식:** `confirmedSeats + offeredSeats <= capacity` 항상 유지.

## 테스트 레이어 규율

| 레이어 | 테스트 종류 | 제약 |
|---|---|---|
| `domain` | 순수 단위 테스트 | Spring, DB, Testcontainers 사용 금지 |
| `application` | `support/FakeRepositories` 기반 단위 테스트 | Spring 컨텍스트, 실제 DB 사용 금지 |
| `adapter.in` | 통합 테스트 | 실제 MySQL 컨테이너 (Testcontainers), Spring 컨텍스트 로드 |
| `adapter.out` | 통합 테스트 | JPA/MySQL 라운드트립, 컨테이너 기반 |

## PR 체크리스트 (`docs/architecture.md` 기준)

- `domain` 레이어에 Spring/JPA/Web import 없음
- `application` 레이어가 `adapter.out` 구체 클래스에 직접 의존하지 않음
- write 유스케이스가 아이덤포턴시/409/422 정책 준수
- 트랜잭션 경계가 `application`에 있음
- 새 예외 코드에 domain 에러코드 + API 응답 매핑이 함께 포함됨
- 새 잡이 `application` 서비스 호출 패턴 준수
- `./gradlew test` 통과

## 알려진 커버리지 미비 항목

- `AuditEventTest` 미존재 — 감사 커버리지는 예약 통합 테스트에서 부분적으로만 검증됨
- `CommunicationReportingIntegrationTest`, `OccurrenceCatalogControllerIntegrationTest` — `main` 브랜치에서 기존부터 실패 중 (이 브랜치 원인 아님)
