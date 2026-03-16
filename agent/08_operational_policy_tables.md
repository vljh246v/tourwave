# Operational Policy Tables (MVP+)

이 문서는 `01_domain_rules.md`를 구현 가능한 규칙으로 세분화한 **운영 정책 표**다.

우선순위:

1. `01_domain_rules.md` (도메인 원칙)
2. `08_operational_policy_tables.md` (정책표)
3. `04_openapi.yaml` (계약)

---

## 1) Refund Policy Matrix

기본 통화는 `occurrence.price_currency` 기준이며, 금액은 minor unit(`KRW` 등)으로 계산한다.

| Trigger | Actor | Time Window (occurrence.start_at_utc 기준) | Booking Status Precondition | Refund Decision | Payment Status Target | Error Code on Reject |
|---|---|---|---|---|---|---|
| Leader cancellation | Leader | `>= 48h` | `CONFIRMED` | `FULL_REFUND` | `REFUND_PENDING -> REFUNDED` | - |
| Leader cancellation | Leader | `< 48h` | `CONFIRMED` | `NO_REFUND` | `PAID` 유지 | - |
| Org cancellation (booking level) | Operator | Any | `REQUESTED, WAITLISTED, OFFERED, CONFIRMED` | `FULL_REFUND` (if paid/authorized) | `REFUNDED` | - |
| Occurrence cancellation | Operator/System | Any | non-terminal booking | `FULL_REFUND` | `REFUNDED` | - |
| Offer decline | Leader | offer 유효 기간 내 | `OFFERED` | `FULL_REFUND` (authorized hold release) | `REFUNDED` | `OFFER_EXPIRED` if expired |
| Offer timeout | System job | `offer_expires_at_utc < now` | `OFFERED` | hold release | `REFUNDED` | - |
| Party size decrease | Leader | Any | `CONFIRMED` | `NO_REFUND` | `PAID` 유지 | `PARTY_SIZE_INCREASE_NOT_ALLOWED` |

정책 메모:

- `REQUESTED/WAITLISTED`에서 아직 capture되지 않았다면 환불 대신 hold release로 처리한다.
- `CANCELED` 재요청은 idempotent 처리하고 중복 환불을 금지한다.
- 결제 게이트웨이 오류로 환불 실패 시 `REFUND_PENDING`으로 남기고 재시도 job 대상이 된다.

---

## 2) Payment Failure & Compensation Matrix

| Step | Failure Point | User-visible Result | Internal Action | Retry Policy | Final Escalation |
|---|---|---|---|---|---|
| Booking approve -> capture | gateway timeout | `202 accepted-like` or domain pending | booking stays `REQUESTED` (or internal pending flag) | exponential backoff (max 5) | operator queue |
| Offer accept -> capture | capture failed | accept rejected | booking rollback to `OFFERED` if offer valid else `EXPIRED` | immediate 1 + job retry | operator queue |
| Refund on cancel | refund API failed | cancel success + refund pending notice | payment `REFUND_PENDING` | job retry every 10m, max 24h | operator/manual refund |
| Duplicate callback/webhook | repeated event | no duplicated side effect | dedupe by `provider_event_id` unique key | no retry needed | alert only |

규칙:

- 외부 결제 호출 성공 전에는 terminal 전이를 확정하지 않는다(또는 보상 트랜잭션 준비).
- `payment_transactions`(권장) 테이블로 request/response/failure_reason을 남긴다.
- 같은 booking에 대해 동시에 두 개의 capture/refund가 실행되지 않도록 row-level lock 또는 distributed lock을 사용한다.

---

## 3) Idempotency Policy Matrix

헤더: `Idempotency-Key` (UUID v4 권장), 사용자 범위: `(actor_user_id, method, path_template, key)`

TTL: 24시간(최소), body hash 일치 필요.

| Endpoint | Required | Dedup Success Response | Conflict Condition | Error Code |
|---|---|---|---|---|
| `POST /occurrences/{occurrenceId}/bookings` | Yes | 최초 생성 booking 반환 | 동일 key + 다른 body | `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD` |
| `POST /bookings/{bookingId}/approve` | Yes | 기존 승인 결과 재반환 | terminal 상태에서 의미 충돌 | `INVALID_BOOKING_STATE` |
| `POST /bookings/{bookingId}/cancel` | Yes | 이미 취소된 결과 재반환 | 환불 중복 시도 | `PAYMENT_ALREADY_REFUNDED` |
| `POST /bookings/{bookingId}/offer/accept` | Yes | 이미 확정된 결과 재반환 | 만료 후 재시도 | `OFFER_EXPIRED` |
| `POST /bookings/{bookingId}/offer/decline` | Yes | 이미 만료/거절 상태 재반환 | 만료 후 의미 없는 재시도 | `INVALID_BOOKING_STATE` |
| `POST /occurrences/{occurrenceId}/inquiries` | Yes | 기존 inquiry 반환(정책상 1건) | 다른 bookingId로 key 재사용 | `BOOKING_SCOPE_MISMATCH` |
| `POST /inquiries/{inquiryId}/messages` | Yes | 최초 생성 메시지 재반환 | 동일 key + 다른 body | `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD` |
| `POST /inquiries/{inquiryId}/close` | Yes | 이미 완료된 close 결과 재반환 | 동일 key + 다른 body | `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD` |

응답 규칙:

- 재요청 hit 시 **원본 status code + body 그대로** 반환.
- 처리 중 중복요청은 `409 IDEMPOTENCY_IN_PROGRESS` 허용.

---

## 4) Time & Timezone Policy

| Item | Policy |
|---|---|
| 서버 저장 | 모든 시각은 UTC (`*_at_utc`) |
| 표시용 timezone | occurrence에 IANA timezone 저장 (예: `Asia/Seoul`) |
| N시간 규칙 계산 | occurrence local time으로 변환 후 경계 계산 |
| 경계 포함 여부 | `now >= start - N hours` 이면 차단 시작(포함) |
| invitation 만료 시각 | `min(invited_at + 48h, occurrence.start_at_utc)` |
| offer 만료 비교 | `now > offer_expires_at_utc` 면 만료 |
| clock source | app server 단일 기준(clock abstraction) |

DST/윤초 메모:

- DST 전환일 테스트를 반드시 포함한다.
- timezone 변환은 DB 함수 혼용 대신 애플리케이션 레이어 한 곳에서 처리한다.

---

## 5) Waitlist Fairness Policy

기본은 FIFO+fit이지만 starvation 완화를 위해 아래 보조 정책을 적용한다.

| Rule | Description |
|---|---|
| Primary ordering | `created_at ASC` |
| Fit check | `party_size <= available_seats` |
| Skip counter | fit 실패로 스킵될 때 `skip_count += 1` |
| Priority boost | `skip_count >= 3` 이고 다음 사이클에서도 fit이면 우선 promoted |
| Manual override | operator가 특정 booking 강제 offer 가능(사유 기록 필수) |
| Hard guard | 어느 경우에도 `confirmed + offered > capacity` 금지 |

운영 지표 권장:

- 평균 wait 시간
- party_size별 promotion 편차
- manual override 비율

---

## 6) Audit Log Policy (Immutable)

`admin_notes`만으로는 부족하므로 `audit_events`(권장) 이벤트 로그를 둔다.

| Field | Required | Description |
|---|---|---|
| id | Yes | event id |
| occurred_at_utc | Yes | 이벤트 발생 시각 |
| actor_type | Yes | `USER`, `OPERATOR`, `SYSTEM`, `JOB` |
| actor_id | Conditional | 사용자/운영자일 때 필수 |
| action | Yes | 예: `BOOKING_APPROVED`, `OFFER_ACCEPTED`, `REFUND_RETRY` |
| resource_type | Yes | `BOOKING`, `OCCURRENCE`, `INQUIRY`, `PAYMENT` |
| resource_id | Yes | 대상 id |
| before_json | Optional | 변경 전 스냅샷(민감정보 마스킹) |
| after_json | Optional | 변경 후 스냅샷(민감정보 마스킹) |
| reason_code | Optional | 운영 사유 코드 |
| request_id | Optional | trace 연계 |

규칙:

- audit row는 update/delete 금지(append-only).
- 개인정보는 최소화/마스킹 후 저장.
- 운영자 수동 조작(manual offer, force expire, force cancel)은 reason code 없으면 거부한다.

---

## 7) Domain Error Mapping Supplements

`04_openapi.yaml`의 `x-error-code-map`과 함께 아래를 추가 가이드로 사용한다.

| HTTP | code | Meaning | Typical endpoint |
|---|---|---|---|
| 409 | `INVALID_BOOKING_STATE` | 허용되지 않은 상태 전이 | `/bookings/{bookingId}/approve` |
| 409 | `OFFER_EXPIRED` | offer 만료 후 accept/decline | `/bookings/{bookingId}/offer/accept` |
| 409 | `CAPACITY_INSUFFICIENT` | 수용 가능 좌석 부족 | `/bookings/{bookingId}/offer/accept` |
| 409 | `IDEMPOTENCY_IN_PROGRESS` | 동일 키 요청 처리 중 | write 계열 POST |
| 422 | `BOOKING_SCOPE_MISMATCH` | occurrence/org와 booking 불일치 | `/occurrences/{occurrenceId}/inquiries` |
| 422 | `PARTY_SIZE_INCREASE_NOT_ALLOWED` | partySize 증가 시도 | `/bookings/{bookingId}/party-size` |
| 422 | `INVITE_WINDOW_CLOSED` | 시작 N시간 이내 초대 생성 시도 | `/bookings/{bookingId}/participants/invite` |

---

## 8) Minimal Rollout Checklist

- 환불/결제 재시도 job 배치 완료
- idempotency 저장소 + TTL purge job 완료
- audit_events append-only 보장 완료
- timezone 경계 테스트(DST 포함) 완료
- waitlist starvation 모니터링 대시보드 준비
