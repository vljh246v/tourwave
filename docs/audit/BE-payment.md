# BE 감사: payment

마지막 감사: 2026-04-18

## 요약
- 구현 완성도: 🟡 (webhook 수신 구현됨, 결제 제공자 어댑터 미구현)
- 테스트 완성도: 🟡 (webhook, reconciliation 테스트 있음, provider 어댑터 테스트 미비)
- OpenAPI path 수: 3개 (태그: Payments)
- SSOT 참조: openapi.yaml 태그 Payments

## Domain 레이어
### 엔티티
- [x] `PaymentRecord` — `src/main/kotlin/com/demo/tourwave/domain/payment/PaymentRecord.kt`
  - 결제 생명주기 추적: AUTHORIZED → CAPTURED 또는 REFUND_PENDING → REFUNDED
  - 환불 재시도 상태 관리: nextRetryAtUtc, refundRetryCount, lastRemediationAction
  - 제공자 참조 저장: providerPaymentKey, providerAuthorizationId, providerCaptureId

- [x] `PaymentProviderEvent` — webhook 수신 이벤트 기록
  - eventType: AUTHORIZED, CAPTURED, CAPTURE_FAILED, AUTHORIZATION_CANCELED, REFUNDED, REFUND_FAILED
  - signature 검증, payload SHA256 저장

- [x] `PaymentReconciliationDailySummary` — 일일 대사 요약
  - 상태별 카운트, 제공자 이벤트 카운트, 불일치 카운트

### 상태 머신
- [x] `PaymentRecordStatus` — AUTHORIZED, CAPTURED, REFUND_PENDING, REFUNDED, NO_REFUND, REFUND_FAILED_RETRYABLE, REFUND_REVIEW_REQUIRED
- [x] `PaymentProviderEventType` — AUTHORIZED, CAPTURED, CAPTURE_FAILED, AUTHORIZATION_CANCELED, REFUNDED, REFUND_FAILED
- [x] `PaymentProviderEventStatus` — RECEIVED, PROCESSED, IGNORED_DUPLICATE, REJECTED_SIGNATURE, MALFORMED_PAYLOAD, POISONED, IGNORED
- [x] `RefundRemediationAction` — RETRY, MARK_REVIEW_REQUIRED

### 값 객체
- (domain layer에는 값 객체 불필요, 모두 DTO로 관리)

### 도메인 서비스
- (별도 도메인 서비스 없음, refund policy는 booking 도메인 소속)

### 도메인 이벤트
- 감사 로그:
  - PAYMENT_RECORD_CREATED
  - PAYMENT_RECORD_STATUS_CHANGED
  - REFUND_INITIATED
  - REFUND_COMPLETED
  - REFUND_FAILED
  - REFUND_REMEDIATED

## Application 레이어
### 서비스
- [x] `PaymentWebhookService`:
  - `receive(PaymentWebhookCommand)` — webhook 수신, 서명 검증, 중복 체크, 상태 전이 처리
  - 지원 제공자: 구조만 구현됨 (webhookSecrets 파라미터로 dynamic 로드)
  - 서명 검증: HMAC-SHA256 기반 (parseSecret() helper)

- [x] `ReconciliationService`:
  - `refreshDailySummary(LocalDate)` — 일일 대사 계산 및 저장
  - `getDailySummary(LocalDate)` — 캐시된 요약 조회 또는 갱신
  - `listDailySummaries(LocalDate, LocalDate)` — 범위 내 요약 조회
  - `listMismatches(LocalDate, LocalDate)` — 3가지 불일치 유형 감지

- [x] `RefundOperationsService`:
  - `listRefundOpsQueue()` — REFUND_PENDING, REFUND_FAILED_RETRYABLE, REFUND_REVIEW_REQUIRED 상태 조회
  - `remediateBookingRefund(Long, RefundRemediationCommand)` — 수동 개입 (RETRY 또는 MARK_REVIEW_REQUIRED)

### Port 인터페이스
- [x] `PaymentProviderEventRepository` — webhook 이벤트 저장소
- [x] `PaymentRecordRepository` — 결제 기록 저장소
- [x] `PaymentReconciliationSummaryRepository` — 일일 요약 저장소
- [ ] `PaymentProviderPort` — **미구현** (인터페이스만 정의, 어댑터 없음)

## Adapter.in.web
### 컨트롤러
- [x] `PaymentOperatorController`:
  - `GET /operator/payments/refunds/ops` → listRefundOpsQueue()
  - `POST /operator/payments/bookings/{bookingId}/refund-retry` → remediateBookingRefund(RETRY)
  - `POST /operator/payments/bookings/{bookingId}/refund-review` → remediateBookingRefund(MARK_REVIEW_REQUIRED)

- [x] `PaymentWebhookController`:
  - `POST /webhooks/payments/<provider>` → receive() 
  - 모든 provider webhook 통합 (동적 라우팅)

## Adapter.out.persistence
### JPA 엔티티
- [x] `PaymentRecordJpaEntity` — 테이블 `payment_records`
  - 인덱스: idx_payment_records_booking (unique)
  - 모든 timestamp UTC

- [x] `PaymentProviderEventJpaEntity` — 테이블 `payment_provider_events`
  - 인덱스: idx_payment_provider_events_provider_event_id
  - payloadSha256, signature, signatureKeyId 저장

- [x] `PaymentReconciliationDailySummaryJpaEntity` — 테이블 `payment_reconciliation_daily_summaries`
  - 캐시 성격, summaryDate 당 1개 레코드

### 어댑터 구현
- [x] `JpaPaymentRecordRepositoryAdapter` (구현 포트: `PaymentRecordRepository`)
- [x] `JpaPaymentProviderEventRepositoryAdapter` (구현 포트: `PaymentProviderEventRepository`)
- [x] `JpaPaymentReconciliationSummaryRepositoryAdapter` (구현 포트: `PaymentReconciliationSummaryRepository`)

## Tests
### 단위
- `PaymentWebhookServiceTest` — webhook 수신, 서명 검증, 상태 전이 로직

### 통합
- `PaymentControllerIntegrationTest` — HTTP webhook 수신 계약 테스트
- `HttpPaymentProviderAdapterTest` — **미구현** (PaymentProviderPort 어댑터 테스트 없음)

### 실패 중
- (모두 통과)

## 관찰된 문제
- ⚠️  **PaymentProviderPort 인터페이스만 정의됨, 구현체 없음**
  - 지위치: `src/main/kotlin/com/demo/tourwave/application/payment/PaymentProviderPort.kt`
  - 현재 booking의 PaymentLedgerService에서 직접 refund 처리 안 됨 (TODO 상태)
  - 실제 결제 게이트웨이(Stripe, Tosspayments 등) 연동 대기

- ⚠️  **Webhook 서명 검증 구현 있으나 테스트 커버리지 낮음**
  - HMAC-SHA256 검증 로직 구현됨
  - 실제 제공자별 검증 키 설정 및 테스트 필요

- ⚠️  **대사(Reconciliation) 로직이 READ 전용**
  - 불일치 감지는 가능하나 자동 수정 불가
  - 수동 개입(RemediationAction)은 Booking 환불에만 적용되고 PaymentRecord 상태는 별도 관리

- ⚠️  **결제 캡처(Capture) 프로세스 미존재**
  - AUTHORIZED 상태만 기록, CAPTURED 상태로 전이하는 메커니즘 없음
  - Webhook 이벤트 기반 수동 처리만 가능

- ✅ 모든 timestamp UTC 사용
- ✅ Domain 레이어 Spring/JPA 임포트 없음
- ✅ Application 레이어 adapter.out 구체 클래스 직접 임포트 없음 (port 의존)
- ✅ Webhook 서명 검증 (HMAC-SHA256) 구현
- ✅ 중복 webhook 이벤트 감지 (duplicate check)
