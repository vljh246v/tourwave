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
