# BE 감사: booking

마지막 감사: 2026-04-28 (T-100)

## 요약
- 구현 완성도: ✅
- 테스트 완성도: ✅
- OpenAPI path 수: 14개 (태그: Bookings)
- SSOT 참조: openapi.yaml 태그 Bookings

## Domain 레이어
### 엔티티
- [x] `Booking` — `src/main/kotlin/com/demo/tourwave/domain/booking/Booking.kt`
  - 상태머신 포함: 8개 상태 (REQUESTED, WAITLISTED, OFFERED, CONFIRMED, REJECTED, CANCELED, EXPIRED, COMPLETED)
  - 핵심 메서드: create(), offer(), acceptOffer(), declineOffer(), approve(), reject(), cancel(), complete(), decreasePartySize(), skipWaitlist()

### 상태 머신
- [x] `BookingStatus` — 상태: REQUESTED, WAITLISTED, OFFERED, CONFIRMED, REJECTED, CANCELED, EXPIRED, COMPLETED
  - isTerminal() 유틸리티: REJECTED, CANCELED, EXPIRED, COMPLETED
- [x] `PaymentStatus` — 상태: AUTHORIZED, PAID, REFUND_PENDING, REFUNDED
- [x] `AttendanceStatus` — 상태: UNKNOWN, ATTENDED, NO_SHOW

### 값 객체
- [x] `RefundDecision` — type, reasonCode, refundable 속성
- [x] `RefundPolicyContext` — action, bookingStatus, paymentStatus, occurrenceStartsAtUtc, occurrenceTimezone, evaluatedAtUtc
- [x] `RefundPolicyAction` enum — LEADER_CANCEL, OCCURRENCE_CANCEL, BOOKING_REJECTED, OFFER_DECLINED, OFFER_EXPIRED
- [x] `RefundDecisionType` enum — FULL_REFUND, NO_REFUND, REFUND_PENDING
- [x] `RefundReasonCode` enum — 7개 코드

### 도메인 서비스
- [x] `BookingRefundPolicy` — evaluate() 함수로 환불 정책 평가
  - 48시간 전 취소: 전액 환불
  - 48시간 이내: 환불 불가
  - Occurrence 취소/Booking 거절/Offer 거절/Offer 만료: 전액 환불

### 도메인 이벤트
- 감사 로그 (AuditEventPort 통해 기록):
  - BOOKING_CREATED
  - BOOKING_APPROVED
  - BOOKING_REJECTED
  - BOOKING_CANCELED
  - BOOKING_COMPLETED
  - OFFER_ACCEPTED
  - OFFER_DECLINED
  - OFFER_EXPIRED
  - OCCURRENCE_CANCELED
  - OCCURRENCE_FINISHED

## Application 레이어
### 서비스
- [x] `BookingCommandService`:
  - `createBooking(CreateBookingCommand)` — 예약 생성, idempotency-key 필수, occurrence 행 락, 정원 자동 계산
  - `finishOccurrence(FinishOccurrenceCommand)` — 투어 완료 처리, 감사 로그 기록
  - `cancelOccurrence(CancelOccurrenceCommand)` — 투어 취소, 모든 non-terminal 예약 자동 취소 및 환불 처리
  - `mutateBooking(MutateBookingCommand)` — 예약 승인/거절/취소/완료/파티 크기 조정/오퍼 수락-거절

- [x] `BookingQueryService`:
  - `getBookingDetail(GetBookingDetailQuery)` — 예약 상세 조회, 접근 제어 검증

- [x] `BookingRefundPreviewService`:
  - 환불 금액 사전 계산 기능

- [x] `OfferExpirationService`:
  - offer 만료 처리 (배경 잡)

- [x] `WaitlistOperatorService`:
  - 대기열 관리 및 승격 로직

- [x] `RefundRetryService`:
  - 환불 재시도 오케스트레이션

- [x] `PaymentLedgerService`:
  - 예약 결제 생명주기 관리, refund policy 적용

### Port 인터페이스
- [x] `BookingRepository` — find(), save(), findByOccurrence*, lock() 메서드
- [x] `OccurrenceRepository` — lock(), getOrCreate(), save() 메서드
- [x] `PaymentRecordRepository` — find*(), save(), findByStatuses() 메서드
- [x] `RefundExecutionPort` — 결제 처리 어댑터
- [x] `BookingParticipantRepository` — 참여자 관리
- [x] `IdempotencyStore` — Idempotency-Key 처리
- [x] `AuditEventPort` — 감사 로그 기록

## Adapter.in.web
### 컨트롤러
- [x] `BookingCommandController`:
  - `POST /occurrences/{occurrenceId}/bookings` → createBooking() (idempotency-key 필수)
  - `POST /bookings/{bookingId}/approve` → mutateBooking(APPROVE)
  - `POST /bookings/{bookingId}/reject` → mutateBooking(REJECT)
  - `POST /bookings/{bookingId}/cancel` → mutateBooking(CANCEL)
  - `POST /bookings/{bookingId}/offer/accept` → mutateBooking(OFFER_ACCEPT)
  - `POST /bookings/{bookingId}/offer/decline` → mutateBooking(OFFER_DECLINE)
  - `PATCH /bookings/{bookingId}/party-size` → mutateBooking(PARTY_SIZE_PATCH)
  - `POST /occurrences/{occurrenceId}/cancel` → cancelOccurrence()
  - `POST /occurrences/{occurrenceId}/finish` → finishOccurrence()

- [x] `BookingQueryController`:
  - `GET /bookings/{bookingId}` → getBookingDetail()

- [x] `BookingRefundPreviewController`:
  - 환불 금액 미리보기 API

## Adapter.out.persistence
### JPA 엔티티
- [x] `BookingJpaEntity` — 테이블 `bookings`
  - 인덱스: idx_bookings_occurrence, idx_bookings_occurrence_status, idx_bookings_status_offer
  - 모든 timestamp UTC

### 어댑터 구현
- [x] `JpaBookingRepositoryAdapter` (구현 포트: `BookingRepository`)

## Tests
### 단위
- `BookingTest` — Booking 도메인 엔티티 상태 머신 테스트
- `BookingRefundPolicyTest` — BookingRefundPolicy 환불 정책 평가 로직 테스트
- `PaymentLedgerServiceTest` — 결제 원장 서비스 유닛 테스트

### 통합
- `BookingCommandServiceTest` — idempotency, 환불 적용, 상태 전이
- `BookingQueryServiceTest` — 쿼리 서비스 통합 테스트
- `BookingRefundPreviewServiceTest` — 환불 미리보기 서비스 테스트
- `BookingControllerIntegrationTest` — HTTP 계약 가드 (OpenAPI SSOT)
- `MysqlBookingConcurrencyTest` — Testcontainers 기반 MySQL 동시성 테스트 (단일 occurrence, capacity=1 approve 충돌)
- `BookingCommandServiceLockOrderTest` — lock() → capacity-read 호출 순서 단위 검증 (Spring 미사용) [T-100]
- `BookingConcurrencyAuditTest` — MySQL Testcontainer 동시성 회귀 (100개 동시 create/approve, 데드락 미발생) [T-100]

### 실패 중
- (모두 통과)

## 관찰된 문제
- ✅ Idempotency-Key 헤더 모든 write 엔드포인트에서 필수
- ✅ 감사 로그 모든 상태 전이 및 occurrence 이벤트에 기록
- ✅ Occurrence 행 락 먼저 획득
- ✅ 모든 timestamp UTC 사용
- ✅ 터미널 상태 추가 전이 불가
- ✅ 정원 불변식 검증
- ✅ 환불 정책 48시간 기준 occurrence 타임존 기반 계산
- ✅ Domain 레이어 Spring/JPA 임포트 없음
- ✅ Application 레이어 adapter.out 구체 클래스 직접 임포트 없음
