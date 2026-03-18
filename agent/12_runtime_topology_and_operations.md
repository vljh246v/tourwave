# Runtime Topology And Operations

이 문서는 현재 Tourwave가 어떤 실행 모드와 저장소 구성을 가지는지, 그리고 다음 개발자가 어디를 수정해야 하는지 설명한다.

## 1. Execution Modes

현재 코드는 같은 코드베이스 안에서 두 가지 실행 모드를 가진다.

### API Mode

- entrypoint: `com.demo.tourwave.TourwaveApplication`
- 역할
  - HTTP API 제공
  - web adapter 구동
  - application service orchestration

### Worker Mode

- entrypoint: `com.demo.tourwaveworker.WorkerApplication`
- 역할
  - scheduler / job bootstrap
  - offer expiration
  - invitation expiration
  - refund retry
  - idempotency purge
  - distributed lock coordinated execution

## 2. Current Module Shape

현재는 단일 Gradle 모듈이지만 코드 구조는 이미 분리 방향을 따른다.

- `domain`
- `application`
- `adapter.in.web`
- `adapter.in.job`
- `adapter.out.persistence`
- `adapter.out.persistence.jpa`
- `adapter.out.payment`
- `bootstrap`

즉, 지금은 "same codebase, different runtime entrypoints" 단계다.

## 3. Persistence Profiles

### Default / Non-MySQL Profiles

- in-memory adapters 사용
- 빠른 로컬 검증과 일부 테스트에 적합
- asset storage는 fake adapter, notification channel은 fake email adapter를 사용한다.

### `mysql`

- JPA adapter 사용
- Flyway migration 실행
- MySQL datasource 기준

### `mysql-test`

- JPA adapter 사용
- 현재 환경에서는 H2 MySQL compatibility mode 사용
- 이유
  - Docker provider가 항상 보장되지 않음
- 별도로 real MySQL container smoke test를 CI에서 실행한다.

## 3.1 Runtime Adapter Matrix

- `local`
  - asset: fake storage adapter
  - notification: fake email channel
  - auth fallback: enabled
  - required secrets: 없음, localhost default 사용 가능
- `alpha`, `beta`, `real`
  - asset: HTTP real storage adapter with presign + HEAD verification
  - notification: HTTP email provider adapter
  - auth fallback: disabled by profile policy
  - required secrets:
    - datasource URL / username / password
    - payment base URL / API key
    - notification base URL / API key / sender email
    - asset base URL / public base URL / bucket / access key / secret key
    - app base URL

## 3.2 Fail-Fast Rule

- `alpha`, `beta`, `real` profile은 placeholder default 없이 환경변수를 요구한다.
- 필수 secret이 빠지면 Spring property binding 단계에서 애플리케이션이 기동되지 않아야 한다.
- local/test만 fallback default를 허용한다.

## 4. Important Infrastructure Classes

- `UseCaseConfig`
  - application service bean wiring
- `MysqlPersistenceConfig`
  - datasource / Flyway / JPA 수동 bootstrap
- `TimeWindowPolicyService`
  - timezone-aware 경계 계산
- `OfferExpirationService`
- `InvitedParticipantExpirationService`
- `RefundRetryService`
- `IdempotencyPurgeService`
- `ScheduledJobCoordinator`
  - worker distributed lock + execution metrics + skip/failure tracking
- `WorkerJobLockRepository`
  - in-memory/local and JPA/MySQL adapter 제공

## 5. Worker Design Rules

- job은 도메인 로직을 직접 구현하지 않는다.
- 모든 실제 규칙은 `application` service에 둔다.
- job은 대상 조회, 반복 실행, 스케줄 진입점 역할만 한다.
- 새 job이 필요하면 먼저 `application` use case를 만들고 그다음 `adapter.in.job`을 추가한다.
- 모든 scheduled job은 `ScheduledJobCoordinator`를 통해 distributed lock과 metrics를 거쳐야 한다.

## 6. AWS Deployment Target

제품 수준으로 갈 때의 목표는 다음이다.

- API
  - ECS/Fargate 상시 서비스
- Worker
  - 별도 ECS/Fargate 서비스 또는 EventBridge 스케줄 task
- 공통
  - 같은 DB와 같은 코어 도메인 코드 공유

추가로 필요한 것:

- alert routing and dashboard standardization
- dead-letter style operator queue
- refund retry/operator queue expansion

## 7. Practical Commands

- 전체 테스트
  - `./gradlew test`
- API 회귀 핵심
  - `./gradlew test --tests 'com.demo.tourwave.domain.booking.application.BookingControllerIntegrationTest'`
- persistence 회귀 핵심
  - `./gradlew test --tests 'com.demo.tourwave.adapter.out.persistence.jpa.MysqlPersistenceIntegrationTest'`
- concurrency 회귀 핵심
  - `./gradlew test --tests 'com.demo.tourwave.application.booking.MysqlBookingConcurrencyTest'`
- contract 회귀 핵심
  - `./gradlew test --tests 'com.demo.tourwave.agent.OpenApiContractVerificationTest'`
- real MySQL smoke
  - `./gradlew test --tests 'com.demo.tourwave.adapter.out.persistence.jpa.RealMysqlContainerSmokeTest'`

## 8. Next Infra Steps

- Gradle true multi-module split
- alert routing / dashboard automation
- worker schedule ownership standardization
- observability / operational queue deepening
