# Domain Rules (MVP)

## Role Model
A user can have multiple roles (set):
- USER
- INSTRUCTOR
- ORG_MEMBER
- ORG_ADMIN
- ORG_OWNER

Org roles are granted per Organization membership.

## Entities & Ownership
- Tour belongs to exactly **one Organization**.
- Tour can have **multiple instructors** assigned (instructor profiles).
- TourOccurrence belongs to exactly one Tour (therefore one Organization).

## Booking & Payment (Stub) State Model

### BookingStatus
- REQUESTED: User requested booking; awaiting org approval.
- WAITLISTED: No seats currently; queued by createdAt (FIFO).
- OFFERED: Seats became available; leader must accept within offer window.
- CONFIRMED: Seats confirmed.
- REJECTED: Org rejected the booking request.
- CANCELED: Canceled by leader or org operator.
- EXPIRED: Waitlist offer expired or leader declined the offer.
- COMPLETED: Booking finalized after tour completion.

### AttendanceStatus
Stored separately from BookingStatus.

- ATTENDED: Participant attended the tour.
- NO_SHOW: Participant did not attend.
- UNKNOWN: Attendance not yet marked.

### PaymentStatus (stub)
- AUTHORIZED: Funds reserved (virtual hold).
- PAID: Captured/paid (virtual).
- REFUND_PENDING: Refund in progress (virtual).
- REFUNDED: Refunded (virtual).

### Core Principle

Booking approval and offer acceptance both lead to confirmed seats.

REQUESTED flow:
- On approve: booking -> CONFIRMED, payment -> PAID
- On reject: booking -> REJECTED, payment -> REFUNDED (if previously authorized)

WAITLIST flow:
- WAITLISTED -> OFFERED (system or operator promotion)
- On accept: booking -> CONFIRMED, payment -> PAID
- On decline: booking -> EXPIRED, payment -> REFUNDED
- On expiration: booking -> EXPIRED

## Explicit State Transition Rules

### BookingStatus transitions (allowed)

- REQUESTED -> CONFIRMED, REJECTED, CANCELED
- WAITLISTED -> OFFERED, CANCELED
- OFFERED -> CONFIRMED, EXPIRED, CANCELED
- CONFIRMED -> CANCELED, COMPLETED
- REJECTED -> (terminal)
- EXPIRED -> (terminal)
- CANCELED -> (terminal)
- COMPLETED -> (terminal)

### Terminal state rule

If a booking is in terminal state (`REJECTED`, `EXPIRED`, `CANCELED`, `COMPLETED`):

- no further business transition is allowed
- duplicate commands should be treated as idempotent no-op when possible

### Occurrence lifecycle precedence

If occurrence becomes `CANCELED`, it has higher priority than booking-local flows:

- all non-terminal bookings transition to `CANCELED`
- waitlist/offer processing for that occurrence must stop

If occurrence becomes `FINISHED`:

- new booking creation is not allowed
- completion/review eligibility evaluation may start

## Capacity, Waitlist, and Offers

### Domain invariants (must always hold)

- `partySize >= 1`
- `capacity >= 1`
- `confirmedSeats + offeredSeats <= capacity`
- One active offer per booking at a time
- A booking in `OFFERED` must have non-null `offerExpiresAtUtc`
- A booking not in `OFFERED` should not hold active offered seats
- `booking.organization_id` must match `occurrence -> tour -> organization`

### Capacity
- Each occurrence has a fixed **capacity**.
- Seats consumed by **CONFIRMED bookings’ partySize**.
- partySize is set at booking creation and can later be **decreased** by leader (no refund).
- Partial acceptance is **not allowed**.

### Waitlist entry
If `availableSeats < partySize` at request time:

- booking is created as WAITLISTED
- payment stub is set to AUTHORIZED (virtual hold)

Waitlist ordering:
- FIFO by booking.createdAt
- Only bookings whose partySize fits into available seats are promoted.

Request-time booking status decision:

- if `availableSeats >= partySize`: create booking as `REQUESTED`
- if `availableSeats < partySize`: create booking as `WAITLISTED`

### Offer promotion
When seats become available:

- System automatically promotes the earliest WAITLISTED booking that fits capacity.
- Booking status becomes OFFERED.
- Offer window defaults to 24 hours (offerExpiresAtUtc).

Manual operator override:
- Operators may manually create an OFFER for a specific booking.
- Manually offered bookings are excluded from automatic promotion until resolved.

### Offer acceptance / decline
Leader may respond while booking status is OFFERED.

Leader actions:
- accept -> booking becomes CONFIRMED, payment -> PAID
- decline -> booking becomes EXPIRED, payment -> REFUNDED

If the offer window expires:
- booking becomes EXPIRED automatically

Offer validity rule:

- accept/decline is allowed only when `now <= offerExpiresAtUtc`
- after expiration, accept/decline must fail with domain conflict

### Manual operator control
Operators may manage waitlist manually (phone/email workflows).

Available actions:
- Manually create OFFER for a specific waitlisted booking.
- Skip a booking and move to the next candidate.

Rules:
- A booking with an active OFFER is excluded from automatic promotion.
- Operator actions must be recorded via admin notes.

## Participant Invitations (Party)

### When invitations are allowed
Invitations can be created only when booking is CONFIRMED.

Constraints:
- Leader cannot create invitations within N hours before start.
- MVP default: N = 6 hours.

Invitation lifecycle:
- Invitation expires after 48 hours (configurable) or sooner if tour start is earlier.
- If invitation expires, leader may send a new invitation.

Important rule:
- Invitation creation is blocked within the 6-hour window.
- Existing invitations remain valid until their expiration.
- Invitation expiration affects participant status only (booking status is unchanged).

### Decline & partySize
- If invited participant declines or expires:
  - **partySize stays unchanged**
  - leader may decrease partySize (allowed; no refund) to free seats.

### Access rules
- All booking participants (leader + accepted members) can view the booking and the inquiry ticket.
- Invitees can accept/decline via participantId endpoint (deep link friendly).

## Inquiry Ticket (투어 상담 티켓)

Inquiry is tied to a booking and its occurrence.

Rules:
- Inquiry can be created only after a booking exists.
- Inquiry create request must include `bookingId`.
- `bookingId` must belong to the same occurrence and organization.
- Participants and organization operators can post messages.
- Attachments are supported via assets.

Capabilities:
- Multiple messages per inquiry
- Optional operator assignment
- Inquiry can be closed by participants or operators

## Occurrence Lifecycle
- SCHEDULED -> FINISHED (operator action) OR CANCELED (operator action).
- If occurrence is canceled:
  - All related bookings are set to CANCELED automatically
  - All payments refunded (virtual)

## Attendance & Completion

Attendance is tracked separately from booking status.

Operator sets attendance per participant:
- ATTENDED
- NO_SHOW
- UNKNOWN

After occurrence is FINISHED:
- booking may transition to COMPLETED.

Reviews require:
- Participant attendanceStatus == ATTENDED
- BookingStatus == COMPLETED (or occurrence finished with business rule).

## Reviews
### Types
- TourReview
- InstructorReview (per instructor)

### Eligibility
Any booking participant can write reviews if:
- participant attendance == ATTENDED
- BookingStatus == COMPLETED (or occurrence finished + business rule)

### Visibility
- Review **content**: author / related instructor / org operators only
- Public: rating average + review count only

### Moderation
- Operators can **hide** a review.

## Cancellation & Refund Policy (Rule-based)
MVP simplified:
- Either **FULL_REFUND** or **NO_REFUND** based on rule thresholds (e.g., 48h).
- Provide preview endpoint to compute outcome for a booking.

## Calendar (ICS)
Provide ICS for booking and occurrence to add to external calendars.

## User Entity & Lifecycle

### UserStatus States

| Status | Description | Recoverable |
|---|---|---|
| `ACTIVE` | 정상 인증된 사용자. 모든 기능 접근 가능. | — |
| `DEACTIVATED` | 사용자 본인 요청으로 비활성화. 로그인 불가, 복구 가능. | Yes (→ ACTIVE) |
| `SUSPENDED` | 운영자 정책 위반 또는 어뷰징으로 운영자가 차단. 복구 가능. | Yes (→ ACTIVE) |
| `DELETED` | 영구 삭제(soft-delete). 개인정보 마스킹 완료. 복구 불가. | No (terminal) |

### State Transition Rules

```
ACTIVE
  ├─→ DEACTIVATED  (actor: 본인, 사유: 계정 비활성화 요청)
  ├─→ SUSPENDED    (actor: 운영자, 사유: 정책 위반)
  └─→ DELETED      (actor: 본인 또는 운영자, 사유: 영구 삭제 요청)

DEACTIVATED
  ├─→ ACTIVE       (actor: 본인, 사유: 계정 복구)
  └─→ DELETED      (actor: 본인 또는 운영자, 사유: 영구 삭제 요청)

SUSPENDED
  ├─→ ACTIVE       (actor: 운영자, 사유: 정지 해제)
  └─→ DELETED      (actor: 운영자, 사유: 영구 삭제)

DELETED → (terminal, 추가 전이 불가)
```

| From \ To | ACTIVE | DEACTIVATED | SUSPENDED | DELETED |
|---|---|---|---|---|
| `ACTIVE` | — | ✓ (self) | ✓ (operator) | ✓ (self/operator) |
| `DEACTIVATED` | ✓ (self) | — | ✗ | ✓ (self/operator) |
| `SUSPENDED` | ✓ (operator) | ✗ | — | ✓ (operator) |
| `DELETED` | ✗ | ✗ | ✗ | — |

**Preconditions:**
- DEACTIVATED → ACTIVE: 사용자가 유효한 인증 자격증명 보유
- SUSPENDED → ACTIVE: 운영자가 명시적으로 정지 해제 처리
- * → DELETED: 현재 상태가 terminal(`DELETED`)이 아닌 경우

**Postconditions:**
- * → DELETED: 개인정보 마스킹 즉시 적용 (아래 마스킹 정책 참조)
- * → DEACTIVATED: 활성 세션(토큰) 즉시 무효화
- * → SUSPENDED: 활성 세션(토큰) 즉시 무효화

### SUSPENDED vs DEACTIVATED 구분 기준

| 구분 기준 | DEACTIVATED | SUSPENDED |
|---|---|---|
| 발동 주체 | 본인 자발적 요청 | 운영자 직권 처리 |
| 사용 사례 | 서비스 일시 탈퇴, 개인 사유 | 어뷰징, 정책 위반, 결제 사기 의심 |
| 복구 주체 | 본인 직접 복구 가능 | 운영자 승인 후 복구 |
| 알림 | 사용자에게 확인 메일 | 사용자에게 사유 및 이의제기 안내 |

### Soft-Delete Masking Policy

`DELETED` 전이 시 아래 필드를 **즉시 마스킹**한다. 마스킹은 비가역적이며 복구 불가.

| Field | Before (example) | After |
|---|---|---|
| `email` | `user@example.com` | `deleted_<userId>@deleted.local` |
| `displayName` | `홍길동` | `Deleted User #<userId>` |
| `passwordHash` | `$2a$10$...` | `[DELETED]` |
| `deletedAt` | `null` | `<삭제 시각 UTC>` |
| `createdAt` | 그대로 유지 | 그대로 유지 (감사 목적) |
| `updatedAt` | 그대로 유지 | 그대로 유지 (감사 목적) |

**구현 예시 (의사 코드):**
```kotlin
fun delete(now: Instant): User {
    return copy(
        status = UserStatus.DELETED,
        displayName = "Deleted User #$id",
        email = "deleted_${id}@deleted.local",
        passwordHash = "[DELETED]",
        deletedAt = now,
        updatedAt = now,
    )
}
```

### Query Filtering Rules

- **기본 쿼리:** `DELETED` 상태 사용자를 결과에서 제외한다.
- **감사/리포트 쿼리:** `includeDeleted = true` 플래그로 모든 상태 포함 가능 (운영자 전용).
- **인증 경계:** `DEACTIVATED`, `SUSPENDED`, `DELETED` 사용자는 인증 단계에서 차단된다 — 유효한 토큰이어도 거부.

```
일반 조회:  WHERE status != 'DELETED'
감사 조회:  (no status filter) — 운영자 권한 확인 후
인증 체크:  status == ACTIVE 만 허용
```

### Audit Events (User Lifecycle)

아래 사용자 상태 변경은 **append-only** 감사 이벤트로 반드시 기록한다.

| Event | Trigger | Actor |
|---|---|---|
| `USER_CREATED` | 회원가입 완료 | SYSTEM |
| `USER_EMAIL_VERIFIED` | 이메일 인증 완료 | USER |
| `USER_PROFILE_UPDATED` | 프로필 수정 (displayName) | USER |
| `USER_DEACTIVATED` | 계정 비활성화 | USER |
| `USER_RESTORED` | 계정 복구 (DEACTIVATED → ACTIVE) | USER |
| `USER_SUSPENDED` | 운영자 정지 처리 | OPERATOR |
| `USER_UNSUSPENDED` | 운영자 정지 해제 | OPERATOR |
| `USER_DELETED` | 영구 삭제 (soft-delete 완료) | USER or OPERATOR |

**감사 이벤트 최소 필드:**
- `actor` (actor_type: USER / OPERATOR / SYSTEM, actor_id)
- `action` (위 이벤트 코드)
- `resource_type`: `USER`
- `resource_id`: user_id
- `occurred_at_utc`
- `before_json` / `after_json`: **PII 필드(email, displayName, passwordHash)는 감사 로그에 기록하지 않는다.** 상태(status)와 비식별 메타데이터만 포함.

> **PII in audit logs 규칙:** `before_json`/`after_json`에 원본 email, displayName, passwordHash를 포함하지 않는다. 이를 포함하면 감사 로그 자체가 PII 저장소가 되어 GDPR 준수 목적이 무효화된다. `status`, `deletedAt` 같은 비식별 필드만 스냅샷에 포함한다.

**예시 이벤트 (USER_DELETED):**
```json
{
  "actor_type": "USER",
  "actor_id": 42,
  "action": "USER_DELETED",
  "resource_type": "USER",
  "resource_id": 42,
  "occurred_at_utc": "2026-04-26T10:00:00Z",
  "before_json": {"status": "ACTIVE"},
  "after_json": {"status": "DELETED", "deleted_at_utc": "2026-04-26T10:00:00Z"}
}
```

## Normative Addendum (Implementation-Blocking Rules)

아래 규칙은 구현 시 필수이며, 위 본문과 동등한 강제력을 가진다.

### A) Idempotency (mandatory)

- 도메인 상태를 변경하는 mutation endpoint는 `Idempotency-Key`를 반드시 요구한다.
- 동일 키 재요청은 최초 응답(status/body)을 그대로 반환해야 한다.
- 동일 키 + 다른 payload는 `422 IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD`로 거부한다.
- 동일 키 처리 중 중복요청은 `409 IDEMPOTENCY_IN_PROGRESS`로 거부할 수 있다.

최소 강제 대상:

- booking create / approve / reject / cancel
- offer accept / decline
- party size patch
- inquiry create
- inquiry message post
- inquiry close

### B) Audit Event (mandatory)

아래 도메인 액션은 append-only audit 이벤트 기록이 필수다.

- booking 상태 변경
- offer 생성/수락/거절/만료
- party size 변경
- occurrence cancel/finish
- inquiry create/close
- payment capture/refund 시도/성공/실패

이벤트 최소 필드:

- actor
- action
- resource_type/resource_id
- occurred_at_utc
- request_id (가능한 경우)

### C) Payment Compensation (mandatory)

- 환불/결제 외부 호출 실패 시 보상 상태를 남겨야 한다.
- 환불 실패는 `REFUND_PENDING`으로 유지하고 retry job 대상이 된다.
- 중복 환불은 금지하며 이미 환불 완료 상태에서 재시도 시 domain conflict로 처리한다.

### D) Time Boundary (mandatory)

- 모든 저장 시각은 UTC 기준이다.
- 경계 규칙(초대 6h 차단, 초대 48h 만료, offer 만료)은 occurrence timezone(IANA) 정책에 맞춰 계산한다.
- 기본 경계:
  - invitation create blocked when `now >= start - 6h`
  - offer expired when `now > offerExpiresAtUtc`

### E) Cross-document consistency

구현 시 아래 문서와 동일한 의미로 해석되어야 한다.

- `04_openapi.yaml` (contract + error examples)
- `06_implementation_notes.md` (guardrails + checklist)
- `08_operational_policy_tables.md` (detailed policy matrix)
