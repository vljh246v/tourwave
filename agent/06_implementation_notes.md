# Implementation Notes (MVP)

This document provides engineering guidance for implementing the Tour Booking Platform backend.

These notes describe important operational behaviors that are not always obvious from the API specification.

---

# Core Implementation Principles

## 0. Architecture constraints are mandatory

All implementation must follow:

- `10_architecture_hexagonal.md`

Non-negotiable constraints:

- Domain/Application layers must remain framework-agnostic.
- IO access must be routed through ports and adapters.
- If architecture conflicts with speed, architecture wins.

## 1. Domain rules are the source of truth

Business behavior must follow:

- `01_domain_rules.md`

Database structure must follow:

- `02_schema_mysql.md`

API contracts must follow:

- `04_openapi.yaml`

Authorization must follow:

- `05_authz_model.md`

If conflicts occur, domain rules take precedence.

If conflicts occur with implementation convenience, architecture constraints still apply.

---

# Hexagonal Refactor Execution Plan (Post-Run)

이미 돌아가고 있는 에이전트 작업이 끝난 후에도, 아래 리팩터 단계를 별도로 수행한다.

## Step A. Layer boundary cleanup

- `domain`에서 Spring/JPA/Web 의존 제거
- `application`에서 유스케이스/트랜잭션 경계 통일
- `adapter.in`에서 Controller/DTO 변환 정리
- `adapter.out`에서 DB/외부 연동 구현 분리

## Step B. Naming + package normalization

- `09_spec_index.md` 용어 기준으로 클래스/메서드 네이밍 정렬
- 패키지를 `domain/application/adapter` 축으로 정리

## Step C. Regression safety

- 상태전이/idempotency/409/422 계약 테스트 유지
- 기존 green 테스트를 깨뜨리지 않는 범위에서 단계적 이동

## Step D. Completion criteria

- `domain -> framework` 의존 0건
- `application -> adapter concrete` 의존 0건
- `./gradlew test` 통과

---

# Transaction Rules

The following operations MUST run inside database transactions:

- booking approval
- booking cancellation
- waitlist promotion
- offer acceptance
- offer expiration
- party size reduction
- occurrence cancellation

Reason:

Seat allocation depends on consistent state.

---

# Occurrence Row Locking

Seat-changing operations must lock the occurrence row.

Example pattern:

SELECT id, capacity
FROM tour_occurrences
WHERE id = :occurrenceId
FOR UPDATE


After locking:

1. recompute seats
2. update booking states
3. commit transaction

This prevents overbooking caused by concurrent operations.

---

# Seat Allocation

Seat allocation must NOT rely on stored counters.

Seats are calculated dynamically.

Formula:

availableSeats = capacity - confirmedSeats - offeredSeats


Where:

confirmedSeats = sum(party_size where booking.status = CONFIRMED)

offeredSeats = sum(party_size where booking.status = OFFERED and offer not expired)

Rules:

- REQUESTED bookings do not consume seats
- WAITLISTED bookings do not consume seats
- OFFERED bookings temporarily hold seats
- CONFIRMED bookings consume seats

---

# Waitlist Promotion

When seats become available:

1. recompute availableSeats
2. find WAITLISTED bookings ordered by createdAt
3. promote bookings that fit capacity

Promotion rule:

if booking.party_size <= availableSeats
booking.status = OFFERED


Then:


booking.offer_expires_at_utc = now + 24h

---

# Offer Handling

When booking status = OFFERED

Leader may:

- accept
- decline

Accept flow:


booking.status = CONFIRMED
payment_status = PAID


Decline flow:


booking.status = EXPIRED
payment_status = REFUNDED


If offer expires:


booking.status = EXPIRED


---

# Participant Invitations

Participant invitations are allowed only when:


booking.status = CONFIRMED


Constraints:

- invitation expires after 48 hours
- invitations cannot be created within 6 hours before tour start

Invitation states:

- INVITED
- ACCEPTED
- DECLINED
- EXPIRED

Invitation does not affect seat allocation.

Seats are controlled by:


booking.party_size


---

# Party Size Reduction

Leader may decrease party size.

Constraints:

- cannot increase party size
- no refund when reducing party size

When reduced:

- seats become available
- waitlist promotion may occur

---

# Attendance Recording

Attendance is recorded per participant.

Values:

- UNKNOWN
- ATTENDED
- NO_SHOW

Attendance does not affect seat allocation.

Attendance is used for:

- reporting
- review eligibility

---

# Inquiry System

Inquiry is tied to:


booking
occurrence
organization


Participants allowed:

- booking leader
- accepted participants
- organization operators

Messages may include:

- text
- attachments

Attachments reference `assets`.

---

# Review System

Two review types exist:

- Tour review
- Instructor review

Review eligibility requires:

- attendance = ATTENDED
- booking completed or occurrence finished

Review visibility:

Public:

- rating average
- review count

Private:

- review body

Visible to:

- review author
- organization operators
- reviewed instructor

---

# Asset Upload

Assets follow a two-step upload model.

Step 1:


POST /assets/upload-url


Server returns a signed upload URL.

Step 2:

Client uploads file to storage.

Step 3:


POST /assets/complete


Asset becomes READY.

---

# Notifications

Notifications are generated for important events.

Examples:

- booking approved
- booking rejected
- waitlist offer created
- offer expired
- participant invited
- occurrence canceled
- inquiry message

Notification channels:

- in-app notifications
- email

---

# Background Jobs

The following background jobs are recommended.

## Offer Expiration Job

Runs periodically.

Logic:


if booking.status = OFFERED
and offer_expires_at_utc < now


Then:


booking.status = EXPIRED


Seats become available.

---

## Orphan Cleanup

Because foreign keys are minimized:

Periodic cleanup should detect:

- orphan inquiry messages
- orphan tour assets
- orphan participants

---

## Denormalization Validation

Verify consistency:


bookings.organization_id


Must match:


occurrence.organization_id


---

# Error Handling

Standard error format:


{
"error": {
"code": "ERROR_CODE",
"message": "Human readable message",
"details": {}
}
}


Common error codes:

- CAPACITY_INSUFFICIENT
- BOOKING_NOT_FOUND
- UNAUTHORIZED
- FORBIDDEN
- INVALID_STATE

---

# Implementation Guardrails (Must)

본 섹션은 개발 실수 방지를 위한 강제 가드레일이다.

정책표 기준 문서:

- `08_operational_policy_tables.md`

## 1) Idempotency Enforcement

Write endpoint(`POST/PATCH/DELETE`) 중 도메인 상태를 변경하는 API는 `Idempotency-Key`를 강제한다.

필수 규칙:

- storage unique key: `(actor_user_id, method, path_template, idempotency_key)`
- body hash가 다르면 재사용 금지
- 재요청 hit 시 최초 응답의 status/body를 그대로 반환

최소 적용 대상:

- create booking
- approve/reject/cancel booking
- offer accept/decline
- inquiry create

## 2) Time Boundary Handling

시간 규칙 계산은 아래 순서를 고정한다.

1. UTC 값을 occurrence timezone(IANA) local datetime으로 변환
2. `N-hour` 경계 계산
3. 비교는 inclusive/exclusive 정책을 명시적으로 적용

기본값:

- invitation blocked window: `now >= start - 6h` 이면 생성 차단
- offer expired: `now > offer_expires_at_utc`

구현 주의:

- DB timezone 함수와 app timezone 계산을 혼용하지 않는다.
- clock abstraction(`Clock` 인터페이스)으로 테스트 고정 시간을 주입한다.

## 3) Payment Compensation

결제/환불 호출 실패 시 booking 전이만 완료시키고 끝내지 않는다.

필수 규칙:

- `payment_status = REFUND_PENDING` 상태를 유지
- retry job이 backoff로 재시도
- 최대 재시도 초과 시 operator action queue로 이관

금지 규칙:

- refund 실패 상태에서 booking을 성공 환불 완료처럼 노출
- 동일 booking에 대해 동시 capture/refund 실행

## 4) Waitlist Fairness & Starvation Protection

FIFO+fit 기본 정책 위에 starvation 완화 규칙을 둔다.

권장 구현:

- `waitlist_skip_count` 컬럼(또는 equivalent view)
- promotion cycle마다 fit 실패 시 count 증가
- `skip_count >= 3` 조건에서 다음 fit 시 우선 검토

운영자 수동 override 시:

- reason code 필수
- audit event 필수

## 5) Occurrence Cancellation Precedence

`occurrence.status = CANCELED` 전이는 booking 로컬 전이보다 우선한다.

필수 구현 순서:

1. occurrence lock
2. 대상 booking 조회(non-terminal)
3. 일괄 `CANCELED`
4. offer/waitlist 후속 job enqueue 중지 또는 무효화
5. 환불/hold release workflow 트리거

## 6) Audit Event Minimum Contract

아래 이벤트는 반드시 audit 로그를 남긴다.

- booking status changed
- offer created/accepted/declined/expired
- party size changed
- occurrence canceled/finished
- inquiry created/closed
- payment capture/refund attempted/failed/succeeded

로그 최소 필드:

- actor, action, resource_type/id, occurred_at_utc, request_id

---

# Endpoint Checklist (Pre-merge)

각 write endpoint PR은 아래를 모두 통과해야 한다.

- [ ] idempotency key 처리
- [ ] terminal state guard
- [ ] occurrence canceled precedence 고려
- [ ] timezone boundary 테스트 포함
- [ ] audit 이벤트 기록
- [ ] 에러코드(`409/422`) 매핑 문서와 일치
