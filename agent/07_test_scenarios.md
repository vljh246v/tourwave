# Test Scenarios (MVP)

This document defines critical end-to-end scenarios used to validate the Tour Booking Platform.

These scenarios should be implemented as integration tests.

---

# Scenario 1 — Basic Booking Approval

Initial state:

Occurrence capacity = 10

Steps:

1. User creates booking with party_size = 2
2. Booking status = REQUESTED
3. Operator approves booking

Expected result:

- booking.status = CONFIRMED
- payment_status = PAID

---

# Scenario 2 — Booking Rejection

Steps:

1. booking.status = REQUESTED
2. operator rejects booking

Expected:

- booking.status = REJECTED
- payment_status = REFUNDED

---

# Scenario 3 — Waitlist Creation

Initial state:

capacity = 10  
confirmed seats = 10

Steps:

1. user creates booking party_size = 2

Expected:

- booking.status = WAITLISTED
- waitlist entry created

---

# Scenario 4 — Waitlist Promotion

Initial state:

capacity = 10  
confirmed seats = 8

Waitlist:

- booking A (party=4)
- booking B (party=2)

Steps:

1. system checks waitlist

Expected:

- booking A skipped
- booking B promoted

Result:

- booking.status = OFFERED

---

# Scenario 5 — Offer Acceptance

Initial state:

booking.status = OFFERED

Steps:

1. leader accepts offer

Expected:

- booking.status = CONFIRMED
- payment_status = PAID

---

# Scenario 6 — Offer Expiration

Initial state:

booking.status = OFFERED  
offer_expires_at_utc < now

Steps:

1. background job runs

Expected:

- booking.status = EXPIRED
- seats released

---

# Scenario 7 — Participant Invitation

Initial state:

booking.status = CONFIRMED

Steps:

1. leader invites participant
2. participant accepts

Expected:

- participant.status = ACCEPTED

---

# Scenario 8 — Invitation Expiration

Steps:

1. participant invited
2. invitation expires

Expected:

- participant.status = EXPIRED

---

# Scenario 9 — Party Size Reduction

Initial state:

booking.party_size = 4

Steps:

1. leader reduces party_size to 2

Expected:

- seats released
- waitlist promotion may occur

---

# Scenario 10 — Inquiry Messaging

Steps:

1. leader opens inquiry
2. operator replies
3. participant replies

Expected:

- messages stored
- notifications sent

---

# Scenario 11 — Review Eligibility

Initial state:

participant attendance = ATTENDED  
occurrence finished

Steps:

1. participant creates review

Expected:

- review created

---

# Scenario 12 — Occurrence Cancellation

Steps:

1. operator cancels occurrence

Expected:

- all bookings status = CANCELED
- payments refunded

---

# Scenario 13 — Participant Roster Export

Steps:

1. operator requests roster export

Expected:

- CSV file generated
- contains confirmed participants

---

# Scenario 14 — Invalid Transition Rejected

Initial state:

booking.status = REJECTED

Steps:

1. operator tries to approve rejected booking

Expected:

- transition rejected (domain conflict)
- booking.status remains REJECTED

---

# Scenario 15 — Offer Accept After Expiration

Initial state:

booking.status = OFFERED  
offer_expires_at_utc < now

Steps:

1. leader calls offer accept

Expected:

- accept rejected (domain conflict)
- booking.status becomes EXPIRED (or already EXPIRED by job)

---

# Scenario 16 — Occurrence Cancellation Precedence

Initial state:

occurrence.status = SCHEDULED

bookings:

- REQUESTED
- WAITLISTED
- OFFERED
- CONFIRMED

Steps:

1. operator cancels occurrence

Expected:

- all non-terminal bookings -> CANCELED
- waitlist promotion/offer flow stopped for occurrence

---

# Scenario 17 — Inquiry Booking Scope Validation

Steps:

1. user creates inquiry with bookingId from different occurrence/org

Expected:

- request rejected (domain validation error)
- inquiry not created

---

# Scenario 18 — Booking Creation Status Decision

Initial state:

capacity = 10

Case A:

- available seats = 3
- request party_size = 2

Expected:

- booking.status = REQUESTED

Case B:

- available seats = 1
- request party_size = 2

Expected:

- booking.status = WAITLISTED

---

# Scenario 19 — Idempotent Booking Create (Same Payload)

Steps:

1. client sends `POST /occurrences/{occurrenceId}/bookings` with `Idempotency-Key=K1`
2. same client retries with same key and same payload

Expected:

- second response equals first response (status/body identical)
- no duplicate booking row

---

# Scenario 20 — Idempotency Key Reused With Different Payload

Steps:

1. send booking create with `Idempotency-Key=K2`, payload A
2. retry with same key `K2`, payload B (different party_size)

Expected:

- request rejected
- error code = `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD`

---

# Scenario 21 — Leader Cancellation Refund Boundary (48h)

Initial state:

booking.status = CONFIRMED

Case A:

- now = start - 49h

Expected:

- cancel success
- refund decision = FULL_REFUND

Case B:

- now = start - 47h 59m

Expected:

- cancel success
- refund decision = NO_REFUND

---

# Scenario 22 — Refund API Failure Compensation

Steps:

1. cancel booking eligible for refund
2. payment provider refund call fails (timeout)

Expected:

- booking remains CANCELED
- payment_status = REFUND_PENDING
- retry job scheduled
- audit event contains failure reason

---

# Scenario 23 — Invitation Window Boundary (6h)

Initial state:

booking.status = CONFIRMED

Case A:

- now = start - 6h exactly

Expected:

- invite create rejected
- error code = `INVITE_WINDOW_CLOSED`

Case B:

- now = start - 6h - 1m

Expected:

- invite create allowed

---

# Scenario 24 — Offer Expiration Boundary

Initial state:

booking.status = OFFERED

Case A:

- now = offer_expires_at_utc exactly

Expected:

- accept allowed (policy: `now <= expiresAt`)

Case B:

- now = offer_expires_at_utc + 1ms

Expected:

- accept rejected
- error code = `OFFER_EXPIRED`

---

# Scenario 25 — DST Boundary for Invite Rule

Initial state:

occurrence.timezone has DST transition day

Steps:

1. compute invite blocked boundary around DST jump
2. attempt invite before/after computed boundary

Expected:

- decision matches local-time policy, not naive UTC subtraction bug

---

# Scenario 26 — Waitlist Starvation Mitigation

Initial state:

capacity churn causes large party to be skipped repeatedly

Steps:

1. run promotion cycles where booking A (party=5) is skipped 3 times
2. ensure A eventually gets priority when fit condition becomes true

Expected:

- skip count tracked
- fairness rule applied
- no capacity over-allocation

---

# Scenario 27 — Concurrent Accept vs Expire Race

Initial state:

booking.status = OFFERED, near expiration

Steps:

1. request A: leader accept offer
2. request B: expiration job picks same booking
3. run concurrently

Expected:

- exactly one terminal outcome
- no double seat allocation
- no duplicate payment side effect

---

# Scenario 28 — Occurrence Cancel Stops Promotion Pipeline

Initial state:

occurrence has WAITLISTED and OFFERED bookings

Steps:

1. operator cancels occurrence
2. promotion job tick happens after cancellation

Expected:

- no new OFFERED bookings created
- all non-terminal bookings remain CANCELED

---

# Scenario 29 — Audit Event Completeness

Steps:

1. execute booking approve -> offer accept -> party size change -> cancel
2. query audit log

Expected:

- one event per domain action exists
- each event has actor/action/resource/occurred_at/request_id
- immutable behavior verified (no update/delete path)

---

# Scenario 30 — Inquiry Idempotent Create

Steps:

1. create inquiry with bookingId and `Idempotency-Key=IQ1`
2. retry same request with same key

Expected:

- same inquiry returned
- no duplicate inquiry row
