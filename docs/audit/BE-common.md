# BE 감사: common

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: ✅ 완성
- 테스트 완성도: ✅ 완성
- OpenAPI path 수: N/A (업무 도메인 아님)
- SSOT 참조: `docs/policies.md` (멱등성, 감사 정책)

## Domain 레이어

### 엔티티
- **WorkerJobLock**: 분산 잠금 (worker 프로세스 간)
  - `lockName`: 잠금 이름 (PK, 100자 max)
  - `ownerId`: 소유자 ID (worker 프로세스 ID, 191자 max)
  - `lockedAtUtc`, `leaseExpiresAtUtc`: 리스 관리
- **JobExecutionSnapshot**: 잡 실행 메타데이터
  - `jobName`, `status` (RUNNING / SUCCESS / FAILURE / SKIPPED)
  - `lastStartedAtUtc`, `lastFinishedAtUtc`, `lastDurationMs`, `lastErrorMessage`
  - `runCount`, `successCount`, `failureCount`, `skippedCount` (누적)

### 상태 머신
- **JobExecutionStatus**: 4개 상태 (RUNNING → SUCCESS / FAILURE / SKIPPED)

### 값 객체
- 없음 (구조 타입만 존재)

### 도메인 서비스
- **DomainException**: 도메인 에러 (errorCode, status, message, details)
- **ErrorCode** enum: 도메인 에러 코드 (VALIDATION_ERROR, BOOKING_TERMINAL_STATE, INVALID_STATE_TRANSITION 등)

### 도메인 이벤트
- 없음

## Application 레이어

### 서비스
- **IdempotencyPurgeService**
  - `purgeExpired()`: 만료된 멱등성 레코드 정리 (TTL 기반)
  - 감사: AuditEventPort.append() 호출 (actor="JOB:0", action="IDEMPOTENCY_PURGED")
- **ScheduledJobCoordinator**
  - `<T> run(jobName, onSkipped, action)`: 분산 잠금 기반 잡 실행
  - 스킵 시: jobExecutionMonitor.recordSkipped()
  - 성공 시: duration, 메트릭 기록 (Micrometer)
  - 실패 시: exception 메시지, 메트릭 기록
- **TimeWindowPolicyService**
  - `isInvitationWindowClosed()`: 초대 6시간 윈도우 판정
  - `isOfferExpired()`: offer 만료 (상수 기반)
  - `is48hInvitationExpired()`: 초대 48시간 만료 판정
- **JobExecutionMonitor**
  - `recordStarted/Success/Failure/Skipped()`: JobExecutionSnapshot 상태 갱신

### Port 인터페이스
- **IdempotencyStore**: 멱등성 키 저장소
  - `reserveOrReplay(actorUserId, method, pathTemplate, idempotencyKey, requestHash)` → IdempotencyDecision
  - `complete(actorUserId, method, pathTemplate, idempotencyKey, status, body)`
  - `markInProgressForTest()`: 테스트용
- **IdempotencyMaintenancePort**: TTL 정리
  - `purgeExpired(nowEpochMillis)` → purgedCount
- **AuditEventPort**: 감사 기록 (모든 도메인에서 사용)
  - `append(AuditEventCommand)`
- **AuditEventSubscriber**: 감사 이벤트 수신자 (구현체: 파일/DB)
- **AuthzGuardPort**: 인증/권한 (모든 도메인에서 사용)
  - `requireActorUserId(actorUserId?)` → Long
- **WorkerJobLockRepository**: 잠금 저장소
  - `tryAcquire(lockName, ownerId, lockedAtUtc, leaseExpiresAtUtc)` → Boolean
  - `release(lockName, ownerId)`

## Adapter.in.web

해당 없음 (HTTP 엔드포인트 없음)

## Adapter.in.job

해당 없음 (scheduler 통합 레이어는 별도)

## Adapter.out.persistence

### JPA 엔티티
- **WorkerJobLockJpaEntity**
  - 테이블: `worker_job_locks`
  - PK: `lock_name`
  - 인덱스: `(lease_expires_at_utc)`
- 없음: JobExecutionSnapshot은 메모리 기반 (JPA 엔티티 아님)

### 어댑터 구현
- **JpaWorkerJobLockRepositoryAdapter**: try-acquire / release 로직
  - select for update with timeout 미사용 (직접 구현)
- **InMemoryWorkerJobLockRepositoryAdapter**: 테스트용 (ConcurrentHashMap)
- **JpaIdempotencyStoreAdapter**: 멱등성 저장소 (별도 감사 필요)
- **InMemoryIdempotencyStoreAdapter**: 테스트용

## Tests

### 단위
- **IdempotencyPurgeServiceTest** (2개 테스트)
  - 만료 레코드 정리
  - 감사 이벤트 기록
- **ScheduledJobCoordinatorTest** (4개 테스트)
  - 잠금 획득 → 액션 실행 → 릴리스
  - 스킵 (잠금 실패)
  - 성공 / 실패 메트릭 기록
  - 예외 처리 (finally에서 릴리스)
- **TimeWindowPolicyServiceTest** (6개 테스트)
  - 초대 6시간 윈도우
  - offer 만료
  - 48시간 초대 만료

### 통합
- 모든 도메인 통합 테스트에서 IdempotencyStore / AuditEventPort 주입되어 사용됨

### 실패 중인 테스트
- 없음

## 관찰된 문제
1. **멱등성 저장소 설계**: IdempotencyStore 인터페이스는 있으나, 실제 구현(JPA/Redis)의 TTL 정책 코드 미검사 (Flyway 마이그레이션 확인 필요)
2. **감사 이벤트 구독자**: AuditEventSubscriber 인터페이스만 있고, 실제 구현이 bootstrap 설정에서 wire 되는지 미확인
3. **TimeWindowPolicyService 상수**: 초대 6시간, 48시간 등의 값이 하드코딩되어 있음 (설정값으로 이동 권장)
4. **JobExecutionMonitor**: 메모리 기반이므로 restart 시 이력 손실 (Micrometer 메트릭만 남음)
