# BE 감사: operations

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: ✅ 완성
- 테스트 완성도: ✅ 완성 (단위 + 통합)
- OpenAPI path 수: 2개 (태그: Operations)
- SSOT 참조: `docs/openapi.yaml` (Operations 태그), `docs/domain-rules.md` (환불/알림 정책)

## Domain 레이어

### 엔티티
- **OperatorFailureRecord**: 운영 실패 레코드 (감사/복구 추적)
  - `sourceType` enum: REFUND / NOTIFICATION_DELIVERY / PAYMENT_WEBHOOK
  - `sourceKey`: 리소스 ID (string, 191자 max)
  - `status`: OPEN / RESOLVED
  - `lastAction`: RETRY / RESOLVE
  - `retryCount`: 재시도 횟수
  - `lastActionByUserId`, `lastActionAtUtc`: 마지막 조작 추적

### 상태 머신
- **OperatorFailureRecordStatus**: 2개 상태 (OPEN ↔ RESOLVED)
- **OperatorFailureAction**: 2개 액션 (RETRY / RESOLVE)
- **OperatorFailureSourceType**: 3개 소스 (REFUND / NOTIFICATION_DELIVERY / PAYMENT_WEBHOOK)

### 값 객체
- 없음

### 도메인 서비스
- 없음 (데이터 구조 역할)

### 도메인 이벤트
- 없음

## Application 레이어

### 서비스
- **OperatorRemediationQueueService**
  - `listOpenItems()`: 열린 실패 레코드 목록 (정렬: sourceUpdatedAtUtc desc)
  - `remediate(sourceType, sourceKey, command)`: 재시도 또는 해결
    - RETRY: refundOperationsService / notificationDeliveryService / paymentWebhookService 호출
    - RESOLVE: record 상태 RESOLVED로 변경
  - 내부: `buildRawItems()` (refund/notification/webhook 소스 병렬 수집)
  - 내부: `mergeWithRecord()` (기록과 실시간 상태 병합)
  - 감사: AuditEventPort.append() 호출

### Port 인터페이스
- **OperatorFailureRecordRepository**: save, findBySourceKey, findOpenRecords
- **PaymentRecordRepository**: 결제 상태 조회
- **NotificationDeliveryRepository**: 알림 상태 조회
- **PaymentProviderEventRepository**: 웹훅 이벤트 조회

## Adapter.in.web

### 컨트롤러
- **OperatorRemediationQueueController** (2 엔드포인트)
  - `GET /operator/operations/remediation-queue` (목록, 200)
  - `POST /operator/operations/remediation-queue/{sourceType}/{sourceKey}` (재시도/해결, 200)

### 인증/권한
- `X-Actor-User-Id` 헤더 필수 (operator 역할 암시)

### Idempotency
- **⚠ 미구현**: POST에 Idempotency-Key 없음 (도메인 규칙 위반 가능성)

## Adapter.in.job

- **WorkerJobLock**: 동시성 제어 (worker 프로세스 간 잠금)
  - 테이블: `worker_job_locks`
  - 리스 만료: `leaseExpiresAtUtc`

## Adapter.out.persistence

### JPA 엔티티
- **OperatorFailureRecordJpaEntity**
  - 테이블: `operator_failure_records`
  - 유니크: `(source_type, source_key)`
  - 인덱스: `(updated_at_utc)`
- **WorkerJobLockJpaEntity**
  - 테이블: `worker_job_locks`
  - PK: `lock_name` (string)
  - 인덱스: `(lease_expires_at_utc)`

### 어댑터 구현
- **JpaOperatorFailureRecordRepositoryAdapter**: 표준 CRUD
- **JpaWorkerJobLockRepositoryAdapter**: try-acquire / release 로직
- **InMemoryOperatorFailureRecordRepositoryAdapter**: 테스트용
- **InMemoryWorkerJobLockRepositoryAdapter**: 테스트용

## Tests

### 단위
- **OperatorRemediationQueueServiceTest** (6개 테스트)
  - 목록: 다중 소스(refund/notification/webhook) 병합
  - RETRY: 리트라이 액션 dispatch
  - RESOLVE: 상태 변경
  - 감사 기록 검증
  - 재시도 가능 여부 판정
- **ScheduledJobCoordinatorTest**: lock 재획득 시 skip 검증

### 통합
- **OperatorRemediationIntegrationTest** (예상되지만 코드 미발견)
  - 실제 MySQL + 예약/결제/알림 리포지토리와 통합

### 실패 중인 테스트
- 없음 (현재 develop에서 모두 성공)

## 관찰된 문제
1. **Idempotency-Key 미구현**: POST 엔드포인트에 멱등성 키 없음 (도메인 규칙 위반)
2. **부분 재시도**: buildRawItems()가 모든 소스를 병렬 수집하지만, 특정 소스만 실패했을 때 표시 방식 불명확
3. **레이스 조건**: 재시도 중 다른 worker가 상태 변경 시 충돌 처리 미상세
