# Tourwave 멀티모듈 분리 설계안과 Sprint 5 세분화

## 1. 목적

- 현재 Tourwave는 단일 Spring Boot 모듈에 `domain`, `application`, `adapter.in`, `adapter.out`, `bootstrap`이 함께 들어 있다.
- Sprint 5부터는 배치성 운영 작업이 본격적으로 추가되므로, API 서버와 운영 워커를 같은 코드베이스 안에서 분리하는 편이 맞다.
- 목표는 별도 제품으로 쪼개는 것이 아니라, 헥사고날 아키텍처를 유지한 채 `실행 모드`를 나누는 것이다.

## 2. 현재 구조 진단

- 장점
  - 도메인과 유스케이스가 이미 `src/main/kotlin/com/demo/tourwave/domain`과 `application` 기준으로 분리돼 있다.
  - 입력 어댑터와 출력 어댑터도 `adapter.in.web`, `adapter.out.persistence`로 어느 정도 정리돼 있다.
  - Sprint 1~4 기능이 대부분 in-memory adapter 기반으로 검증 가능하다.
- 한계
  - 현재는 `TourwaveApplication` 하나만 있어서 API와 배치 실행 경계가 없다.
  - 배치 작업을 넣기 시작하면 scheduler 코드가 API 부트스트랩으로 섞일 가능성이 높다.
  - `adapter.out` 구현과 Spring Boot 의존이 하나의 모듈에 몰려 있어, 향후 worker 전용 실행 구성을 만들기 어렵다.

## 3. 권장 방향

- 같은 저장소를 유지한다.
- Gradle 멀티모듈로 나눈다.
- `core`는 순수 비즈니스 규칙과 포트만 가진다.
- `infra`는 persistence/external adapter 구현을 가진다.
- `api`는 HTTP 입력 어댑터와 API 전용 bootstrap만 가진다.
- `worker`는 scheduler, background job adapter, worker bootstrap만 가진다.

## 4. 목표 모듈 구조

```text
tourwave
├─ settings.gradle.kts
├─ build.gradle.kts
├─ core
│  └─ src/main/kotlin/com/demo/tourwave
│     ├─ domain
│     └─ application
├─ infra
│  └─ src/main/kotlin/com/demo/tourwave/adapter/out
│     ├─ persistence
│     ├─ payment
│     └─ audit
├─ api
│  └─ src/main/kotlin/com/demo/tourwave
│     ├─ adapter/in/web
│     └─ bootstrap/ApiApplication.kt
└─ worker
   └─ src/main/kotlin/com/demo/tourwave
      ├─ adapter/in/job
      └─ bootstrap/WorkerApplication.kt
```

## 5. 모듈별 책임

### `core`

- 포함
  - `domain.*`
  - `application.*`
  - Port interface
  - 공용 command/query DTO
- 제외
  - Spring MVC
  - 스케줄러
  - 저장소 구현체
  - 외부 API 구현체

### `infra`

- 포함
  - `adapter.out.persistence.*`
  - `adapter.out.payment.*`
  - `adapter.out.audit.*`
  - 추후 JPA, Redis, external client 구현
- 역할
  - `core`의 Port 구현
  - Spring Bean 등록을 위한 config 제공

### `api`

- 포함
  - `adapter.in.web.*`
  - API 전용 exception handler
  - API bootstrap
- 역할
  - HTTP 요청을 유스케이스로 변환
  - 인증/권한 해석
  - web contract 유지

### `worker`

- 포함
  - `adapter.in.job.*`
  - scheduler, job runner, lock coordinator
  - worker bootstrap
- 역할
  - 유스케이스를 주기적으로 호출
  - job 실행, 재시도, 운영 알림 orchestration
- 금지
  - 도메인 규칙 직접 구현
  - repository concrete class에 직접 의존

## 6. 의존 구조

- `api -> core`
- `worker -> core`
- `infra -> core`
- `api -> infra`
- `worker -> infra`
- 금지
  - `core -> api`
  - `core -> worker`
  - `core -> infra concrete`
  - `worker -> api`

## 7. 실행 방식

### 로컬

- API
  - `./gradlew :api:bootRun`
- Worker
  - `./gradlew :worker:bootRun`

### AWS

- API
  - ECS/Fargate service로 상시 실행
- Worker
  - 1안: ECS/Fargate service로 상시 실행 후 내부 scheduler 구동
  - 2안: EventBridge Scheduler가 주기적으로 worker task 기동
- 공통 요구사항
  - 분산락 필요
  - 실패 job 재시도 상태 저장 필요
  - CloudWatch 로그 및 metric 필요

## 8. Sprint 5에 먼저 반영할 범위

- 이번 Sprint 5에서 바로 전체 멀티모듈 전환을 끝내는 것은 범위가 너무 크다.
- 따라서 Sprint 5는 아래 수준까지를 목표로 잡는 것이 현실적이다.
  - `settings.gradle.kts`에 멀티모듈 구조 반영
  - `core`, `api`, `worker`, `infra` 골격 생성
  - worker 전용 bootstrap과 job adapter 경계 도입
  - Offer/Invitation expiration, structured audit, timezone policy를 새 구조에 맞춰 구현
- 즉 Sprint 5는 `배치 기능 추가`와 `실행 모드 분리 시작`을 같이 한다.

## 9. Sprint 5 재세분화 원칙

- 각 상위 티켓은 가능한 한 아래 3축으로 쪼갠다.
  - `core/usecase`
  - `worker or infra adapter`
  - `tests`
- 단, Sprint 5 첫 티켓에는 공통 기반 작업인 worker 모듈 뼈대를 함께 둔다.

## 10. `08_operational_policy_tables.md` 반영 메모

- Offer expiration job은 `now > offer_expires_at_utc` 비교를 사용해야 한다.
- Offer timeout 시 actor는 `JOB` 또는 `SYSTEM`으로 남겨야 하며, 결제 상태는 hold release 또는 refund 결과와 정합해야 한다.
- Invitation expiration은 문서상 `min(invited_at + 48h, occurrence.start_at_utc)` 경계를 따른다.
- `now >= start - 6 hours`부터는 초대 생성/응답 차단 정책을 같은 계산 서비스로 처리해야 한다.
- Waitlist 처리 시 어느 경우에도 `confirmed + offered > capacity`를 만들면 안 된다.
- Audit log는 append-only를 전제로 하며, `before_json`, `after_json`, `reason_code`, `request_id`를 구조적으로 저장해야 한다.
- Timezone 정책은 애플리케이션 레이어 한 곳에서 계산해야 하며 DB 함수 혼용은 피한다.
- Sprint 5 범위 밖이지만, 운영정책 표의 `refund retry job`, `TTL purge job`은 이후 `TW-028`에서 이어받는 것이 맞다.

## 11. Jira 기준 Sprint 5 티켓 재세분화

참고:

- 백로그 문서 기준 Phase 5는 `TW-022`~`TW-025`
- 현재 Jira에서는 같은 범위가 `TW-32`~`TW-35`

### `TW-32` Offer expiration background job

- `Subtask A`
  - 제목: `Worker module bootstrap and job scheduling foundation`
  - 범위
    - `settings.gradle.kts` 멀티모듈 등록안 적용
    - `worker` bootstrap entrypoint 생성
    - background job 공통 인터페이스와 scheduler wiring 정의
  - 완료 조건
    - offer expiration job을 worker에서 호출할 수 있다
- `Subtask B`
  - 제목: `Offer expiration use case and sweep query`
  - 범위
    - 만료 대상 offer sweep port
    - offer expiration application use case
    - waitlist 재계산 정책 연결
  - 완료 조건
    - 만료 대상 booking이 일괄 전이된다
    - `confirmed + offered > capacity`가 되지 않는다
- `Subtask C`
  - 제목: `Offer expiration worker adapter and integration tests`
  - 범위
    - worker job adapter
    - 락/중복 실행 방지 초안
    - integration test
  - 완료 조건
    - job 실행 후 상태 변화와 재실행 안전성이 검증된다
    - audit actor가 `JOB` 또는 `SYSTEM`으로 남는다

### `TW-33` Invitation expiration background job

- `Subtask A`
  - 제목: `Invitation expiration sweep port and use case`
  - 범위
    - pending invitation sweep query
    - expiration application use case
    - expired invitation 상태 전이
  - 완료 조건
    - 만료 대상 invitation이 일괄 정리된다
    - `min(invited_at + 48h, occurrence.start_at_utc)` 경계를 따른다
- `Subtask B`
  - 제목: `Invitation expiration worker adapter`
  - 범위
    - worker scheduler 등록
    - job execution log
    - 재실행 시 idempotent 동작
  - 완료 조건
    - worker에서 invitation expiration job이 동작한다
- `Subtask C`
  - 제목: `Invitation expiration regression tests`
  - 범위
    - post-expiration accept/decline 차단
    - 경계 시각 검증
    - integration test
  - 완료 조건
    - batch와 command 경로가 같은 만료 규칙을 사용한다
    - 시작 6시간 전 차단 정책이 유지된다

### `TW-34` Structured audit log expansion

- `Subtask A`
  - 제목: `Structured audit event schema in core`
  - 범위
    - `beforeJson`, `afterJson`, `reasonCode` 필드 추가
    - actor, eventType, occurredAt 표준화
    - 공통 audit payload 모델 정의
  - 완료 조건
    - application 계층에서 구조화된 audit를 발행할 수 있다
    - 수동 운영 액션은 reason code 없이 기록되지 않는다
- `Subtask B`
  - 제목: `Audit persistence adapter and serialization policy`
  - 범위
    - `infra` audit adapter 갱신
    - 직렬화 포맷 정책 확정
    - 대형 payload 처리 기준 정의
  - 완료 조건
    - 핵심 mutation 이벤트가 구조화된 payload로 저장된다
    - append-only 정책을 위반하지 않는다
- `Subtask C`
  - 제목: `Audit coverage tests for booking, participant, inquiry`
  - 범위
    - booking mutation
    - participant invitation/attendance
    - inquiry mutation
  - 완료 조건
    - 이벤트 종류별 payload 검증 테스트가 존재한다

### `TW-35` Timezone-aware policy handling

- `Subtask A`
  - 제목: `Occurrence timezone model and repository support`
  - 범위
    - occurrence timezone 필드 명확화
    - repository adapter 반영
    - 기존 fixture 정리
  - 완료 조건
    - occurrence가 로컬 timezone을 명시적으로 가진다
- `Subtask B`
  - 제목: `Timezone-aware boundary policy refactor`
  - 범위
    - 48시간 refund 정책
    - 6시간 invitation 정책
    - occurrence-local 기준 계산 서비스
  - 완료 조건
    - 모든 경계 계산이 timezone-aware 로직을 사용한다
    - 계산 위치가 application 정책 서비스로 일원화된다
- `Subtask C`
  - 제목: `DST and local-time regression tests`
  - 범위
    - DST 전환 경계
    - 서로 다른 timezone 비교
    - 기존 정책 회귀 테스트
  - 완료 조건
    - timezone 변경이 정책 회귀를 깨지 않는 것이 검증된다

## 12. 구현 순서 권장안

1. `TW-32` 공통 worker 기반부터 시작
2. `TW-33` invitation expiration
3. `TW-34` structured audit
4. `TW-35` timezone-aware policy

이 순서를 권장하는 이유:

- worker 기반이 먼저 있어야 배치성 티켓을 같은 방식으로 구현할 수 있다.
- invitation expiration은 기존 lazy expiration을 실제 background job으로 올리는 작업이라 빠르게 가치가 나온다.
- audit 확장은 이후 운영 관찰성을 올린다.
- timezone 정책은 앞선 job/usecase가 안정화된 뒤 적용하는 편이 회귀 관리가 쉽다.

## 13. 구현 시 체크리스트

- `core`에 Spring MVC, scheduler import를 넣지 않는다.
- worker는 use case를 호출만 하고 규칙을 재정의하지 않는다.
- batch sweep 조건은 port로 숨기고, query 로직을 controller/service에 두지 않는다.
- command 경로와 batch 경로가 동일한 정책 서비스를 공유해야 한다.
- 테스트는 `application`, `adapter.in.job`, `adapter.out`에 나눠서 둔다.
