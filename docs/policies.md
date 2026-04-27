# Policies

Authorization model, operational policies, and trust surface decisions for the Tour Booking Platform.

> Priority order: `docs/domain-rules.md` (domain principles) → this file (policies) → `docs/openapi.yaml` (contract)

---

## 1. Role Model

A user may hold multiple roles simultaneously.

**Global roles** (logical identity; not a single enum column):
- `USER` — authenticated person with no org privilege
- `INSTRUCTOR` — user with an instructor profile

**Organization-scoped roles** (stored in `organization_members.role`):
- `ORG_MEMBER` — active member, no admin privilege
- `ORG_ADMIN` — can manage members and resources
- `ORG_OWNER` — full control; supersedes ORG_ADMIN in all contexts

**Role hierarchy:** `ORG_OWNER ⊇ ORG_ADMIN ⊇ ORG_MEMBER`

A user may belong to multiple organizations with different roles in each.

**Authorization principles:**
1. **Deny by default** — if not explicitly allowed, deny.
2. **Resource ownership first** — derive org ownership from stored data (never trust client-provided IDs).
3. **Role + relationship** — access depends on both role and relationship to resource (booking leader, accepted participant, etc.).
4. **Server resolves authority** — check `booking.organization_id`, `occurrence.organization_id`, etc. from DB.
5. **Minimize auth joins** — denormalized `organization_id` columns (e.g., `bookings.organization_id`) are intentional; set only on server, never by client.

---

## 2. Resource × Action × Role Matrix

Legend: `✓` = allowed · `—` = denied · `L` = booking leader · `P` = accepted participant · `V` = invited participant · `A+O` = ORG_ADMIN or ORG_OWNER

| Resource | Action | Anonymous | Auth User | L / P / V | A+O (org) |
|---|---|---|---|---|---|
| **Auth / Account** | signup / login / refresh / password-reset | ✓ | ✓ | — | — |
| | logout / email-verify / deactivate / delete-me | — | ✓ (owner) | — | — |
| **Me / Profile** | read/update me, bookings, inquiries, favorites, notifications | — | ✓ (owner) | — | — |
| **Organization** | Read public org profile | ✓ | ✓ | — | — |
| | Read org member list | — | — | — | ✓ (M+) |
| | Invite / deactivate members, change role | — | — | — | ✓ |
| | Create org (creator becomes ORG_OWNER) | — | ✓ | — | — |
| **Instructor** | Apply for registration | — | ✓ | — | — |
| | Approve / reject registration | — | — | — | ✓ |
| | Read/write own instructor profile | — | ✓ (owner) | — | — |
| | Read public instructor profile + rating summary | ✓ | ✓ | — | — |
| **Tour** | List / read published tour, content, rating | ✓ | ✓ | — | — |
| | Favorite / unfavorite | — | ✓ | — | — |
| | Create / update / publish / archive | — | — | — | ✓ |
| | Manage instructors, assets, content | — | — | — | ✓ |
| **Occurrence** | Read published occurrence, availability, quote, search | ✓ | ✓ | — | — |
| | Create / update / cancel / finish / reschedule / notify | — | — | — | ✓ |
| | View calendar ICS | — | — | L + P | ✓ |
| **Announcement** | Read public announcements | ✓ | ✓ | — | — |
| | Create / update / delete | — | — | — | ✓ |
| **Booking** | Create booking | — | ✓ | — | — |
| | Read booking detail / explain | — | — | L + P | ✓ |
| | Cancel booking | — | — | L only | ✓ |
| | Decrease party size | — | — | L only | — |
| | Invite participants | — | — | L only | — |
| | Accept / decline participant invite | — | — | V only | — |
| | Accept / decline waitlist offer | — | — | L only | — |
| | Approve / reject / complete / attendance | — | — | — | ✓ |
| | Waitlist promote / force-expire / extend-offer | — | — | — | ✓ |
| **Occurrence Policies** | Read¹ | ✓¹ | ✓¹ | — | ✓ |
| | Write | — | — | — | ✓ |
| **Admin Notes** | Create / read | — | — | — | ✓ |
| **Inquiry** | Create inquiry | — | — | L + P | ✓ |
| | Read inquiry / messages / post message / close | — | — | L + P | ✓ |
| **Review** | Read rating summary | ✓ | ✓ | — | — |
| | Create review (eligible, post-attendance)² | — | — | L + P² | — |
| | Read review body | — | — | author only | ✓ (also reviewed instructor) |
| | Moderate / hide review | — | — | — | ✓ |
| **Asset** | Upload | — | ✓ | — | — |
| | Read | — | ✓ (owner) | — | ✓ if linked to org resource |
| | Delete | — | ✓ (owner) | — | ✓ if business rules allow |
| **Reports** | Booking / occurrence reports + export | — | — | — | ✓ |
| **Moderation** | Platform moderation endpoints | not built (MVP no-build) | — | — | — |

¹ Policy read may be public or authenticated — UX decision. Write is always operator-only.  
² Review requires `attendance_status = ATTENDED` and eligibility rule. See `docs/domain-rules.md`.

**Invited-but-not-accepted participant:** may access participant accept/decline endpoints only. No booking or inquiry visibility until `ACCEPTED`.

---

## 3. Organization-Scoped Rules

### Membership resolution

A user is an active org member when:
- `organization_members.organization_id = targetOrgId`
- `organization_members.user_id = actorUserId`
- `organization_members.status = ACTIVE`

### Membership lifecycle

| Status | Meaning |
|---|---|
| `INVITED` | Invitation created; not yet accepted |
| `ACTIVE` | Full access per role |
| `INACTIVE` | Disabled; no access |

Deactivated accounts are rejected at the security perimeter even with a valid access token.

### Denial behavior

| HTTP | Condition |
|---|---|
| `401` | Token missing, invalid, or expired |
| `403` | Authenticated but insufficient permission |
| `404` | Resource does not exist, or access must not reveal existence |

Prefer `404` over `403` for hidden resources where enumeration risk matters. Use `403` when the resource is expected to be known to the caller.

### Authorization guard chain (recommended order)

1. Authenticate principal
2. Resolve resource ownership (e.g., `bookings.organization_id`)
3. Evaluate relationship (leader / participant / invitee / org operator)
4. Allow or deny

### Security notes

- Never trust client role claims alone — sensitive resource checks must verify org membership from DB/cache.
- Denormalized `organization_id` fields are set only on server; validate on creation/update; reconcile with background jobs.
- Invitation endpoints must validate: invite target identity, invitation status, and expiration time.
- Review body is private; public APIs must never expose it accidentally.
- Asset URLs should use signed URLs or ownership checks, not be blindly public.

---

## 4. Operational Policy Tables

Supplements `docs/domain-rules.md` for booking, payment, idempotency, time, and waitlist rules.

### 4.1 Refund Policy Matrix

Base currency: `occurrence.price_currency`; amounts in minor units (e.g., KRW).

| Trigger | Actor | Time Window (vs `occurrence.start_at_utc`) | Booking Status Precondition | Refund Decision | Payment Status Target |
|---|---|---|---|---|---|
| Leader cancellation | Leader | `>= 48h` before start | `CONFIRMED` | `FULL_REFUND` | `REFUND_PENDING → REFUNDED` |
| Leader cancellation | Leader | `< 48h` before start | `CONFIRMED` | `NO_REFUND` | `PAID` maintained |
| Org cancellation (booking) | Operator | Any | `REQUESTED, WAITLISTED, OFFERED, CONFIRMED` | `FULL_REFUND` (if paid/authorized) | `REFUNDED` |
| Occurrence cancellation | Operator/System | Any | non-terminal booking | `FULL_REFUND` | `REFUNDED` |
| Offer decline | Leader | within offer validity | `OFFERED` | `FULL_REFUND` (hold release) | `REFUNDED` |
| Offer timeout | System job | `offer_expires_at_utc < now` | `OFFERED` | hold release | `REFUNDED` |
| Party size decrease | Leader | Any | `CONFIRMED` | `NO_REFUND` | `PAID` maintained |

Rules:
- `REQUESTED/WAITLISTED` without capture → hold release, not refund.
- `CANCELED` re-requests are idempotent; duplicate refunds are prohibited.
- Gateway errors → `REFUND_PENDING` + retry job.

### 4.2 Payment Failure & Compensation Matrix

| Step | Failure Point | User-visible Result | Internal Action | Retry Policy | Final Escalation |
|---|---|---|---|---|---|
| Approve → capture | gateway timeout | pending | booking stays `REQUESTED` | exponential backoff (max 5) | operator queue |
| Offer accept → capture | capture failed | accept rejected | rollback to `OFFERED` or `EXPIRED` | immediate 1 + job retry | operator queue |
| Refund on cancel | refund API failed | cancel OK + refund pending | payment `REFUND_PENDING` | job retry every 10m, max 24h | operator/manual refund |
| Duplicate webhook | repeated event | no side effect | dedupe by `provider_event_id` | no retry | alert only |

Rules: never finalize terminal transition before external payment call succeeds. Use `payment_transactions` for audit trail. Row-level or distributed lock prevents concurrent capture/refund on the same booking.

### 4.3 Idempotency Policy Matrix

Header: `Idempotency-Key` (UUID v4). Scope: `(actor_user_id, method, path_template, key)`. TTL: 24h min; body hash required.

| Endpoint | Required | Dedup Response | Conflict Error |
|---|---|---|---|
| `POST /occurrences/{occurrenceId}/bookings` | Yes | original booking | `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD` |
| `POST /bookings/{bookingId}/approve` | Yes | prior approval result | `INVALID_BOOKING_STATE` |
| `POST /bookings/{bookingId}/cancel` | Yes | prior cancel result | `PAYMENT_ALREADY_REFUNDED` |
| `POST /bookings/{bookingId}/offer/accept` | Yes | prior accept result | `OFFER_EXPIRED` |
| `POST /bookings/{bookingId}/offer/decline` | Yes | prior state | `INVALID_BOOKING_STATE` |
| `POST /occurrences/{occurrenceId}/inquiries` | Yes | existing inquiry | `BOOKING_SCOPE_MISMATCH` |
| `POST /inquiries/{inquiryId}/messages` | Yes | original message | `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD` |
| `POST /inquiries/{inquiryId}/close` | Yes | prior close result | `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD` |

On dedup hit: return original status code + body unchanged. In-progress duplicate: `409 IDEMPOTENCY_IN_PROGRESS`.

### 4.4 Time & Timezone Policy

| Item | Policy |
|---|---|
| Storage | All timestamps in UTC (`*_at_utc`) |
| Display timezone | IANA timezone stored on occurrence (e.g., `Asia/Seoul`) |
| N-hour rule calculation | Convert to occurrence local time, then compute boundary |
| Boundary inclusion | `now >= start - N hours` → block starts (inclusive) |
| Invitation expiry | `min(invited_at + 48h, occurrence.start_at_utc)` |
| Offer expiry | `now > offer_expires_at_utc` |
| Clock source | Single app server clock abstraction |

DST: include DST transition tests. All timezone conversion in application layer only — do not use DB timezone functions.

### 4.5 Waitlist Fairness Policy

Base: FIFO + fit. Starvation mitigation:

| Rule | Description |
|---|---|
| Primary ordering | `created_at ASC` |
| Fit check | `party_size <= available_seats` |
| Skip counter | `skip_count += 1` on fit-failed skip |
| Priority boost | `skip_count >= 3` + fit in next cycle → prioritized promote |
| Manual override | Operator may force-offer (reason code required) |
| Hard guard | `confirmed + offered <= capacity` always enforced |

Recommended metrics: average wait time; promotion variance by party size; manual override rate.

### 4.6 Audit Log Policy (Immutable)

| Field | Required | Description |
|---|---|---|
| `id` | Yes | event id |
| `occurred_at_utc` | Yes | event timestamp |
| `actor_type` | Yes | `USER`, `OPERATOR`, `SYSTEM`, `JOB` |
| `actor_id` | Conditional | required for user/operator actors |
| `action` | Yes | e.g., `BOOKING_APPROVED`, `OFFER_ACCEPTED`, `REFUND_RETRY` |
| `resource_type` | Yes | `BOOKING`, `OCCURRENCE`, `INQUIRY`, `PAYMENT` |
| `resource_id` | Yes | target id |
| `before_json` | Optional | pre-change snapshot (PII masked) |
| `after_json` | Optional | post-change snapshot (PII masked) |
| `reason_code` | Optional | operator reason code |
| `request_id` | Optional | trace correlation |

Rules: rows are append-only (no update/delete). PII minimized. Manual operator actions (force offer, force expire, force cancel) require a reason code — reject if absent.

### 4.7 Domain Error Code Supplements

Use alongside `docs/openapi.yaml` `x-error-code-map`:

| HTTP | Code | Meaning | Typical Endpoint |
|---|---|---|---|
| 409 | `INVALID_BOOKING_STATE` | Disallowed state transition | `POST /bookings/{bookingId}/approve` |
| 409 | `OFFER_EXPIRED` | Offer expired before accept/decline | `POST /bookings/{bookingId}/offer/accept` |
| 409 | `CAPACITY_INSUFFICIENT` | Insufficient available seats | `POST /bookings/{bookingId}/offer/accept` |
| 409 | `IDEMPOTENCY_IN_PROGRESS` | Duplicate key in flight | write POST endpoints |
| 422 | `BOOKING_SCOPE_MISMATCH` | occurrence/org mismatch with booking | `POST /occurrences/{occurrenceId}/inquiries` |
| 422 | `PARTY_SIZE_INCREASE_NOT_ALLOWED` | Party size increase attempt | `PATCH /bookings/{bookingId}/party-size` |
| 422 | `INVITE_WINDOW_CLOSED` | Invitation within N-hour window | `POST /bookings/{bookingId}/participants/invite` |
| 422 | `ASSET_UNSUPPORTED_CONTENT_TYPE` | MIME type not in allowed whitelist | `POST /assets/uploads` |

---

### 4.8 Asset Upload Policy

#### Content-Type 화이트리스트 (T-204, 2026-04-28)

`POST /assets/uploads` 엔드포인트는 `contentType` 필드를 반드시 허용 목록과 대조한다.

| MIME 타입 | 허용 여부 | 설명 |
|---|---|---|
| `image/jpeg` | ✅ 허용 | JPEG 이미지 |
| `image/png` | ✅ 허용 | PNG 이미지 |
| `image/webp` | ✅ 허용 | WebP 이미지 |
| `image/gif` | ✅ 허용 | GIF 애니메이션 |
| `application/pdf` | ✅ 허용 | PDF 문서 |
| 그 외 모든 타입 | ❌ 거부 | `422 ASSET_UNSUPPORTED_CONTENT_TYPE` |

**구현 위치:** `domain/asset/AssetContentType.kt` — deny by default 원칙.  
**대소문자 처리:** `trim().lowercase()` 정규화 후 비교. `IMAGE/JPEG` → 허용.  
**향후 확장:** magic byte 검증(파일 시그니처)은 별도 카드 — adapter.out 레이어, 업로드 후 비동기.

---

## 5. Trust Surface Policy

### 5.1 User Privacy & Deletion Policy

#### Soft-Delete vs Hard-Delete 선택 근거

Tourwave는 **Soft-Delete** 방식을 채택한다.

| 방식 | 설명 | 채택 여부 |
|---|---|---|
| Soft-Delete | 레코드 유지 + 개인정보 마스킹 | **채택** |
| Hard-Delete | DB 레코드 물리 삭제 | 미래 과제 (GDPR hard-delete 요건 시) |

**Soft-Delete 채택 이유:**
1. **감사 추적 유지:** 예약, 결제, 리뷰 등 연관 레코드의 참조 무결성 보존
2. **규제 대응:** 개인정보보호법 / GDPR 준수를 위한 처리 이력 보존 의무
3. **데이터 복구 용이성:** 실수 또는 법적 분쟁 시 마스킹 전 이력 추적 가능 (감사 로그 기반)
4. **운영 안전성:** FK 참조를 끊지 않으므로 cascade 삭제 버그 위험 없음

#### 마스킹 필드 목록

`DELETED` 전이 시 아래 필드를 즉시 마스킹한다. 마스킹은 비가역적이다.

| 필드 | 마스킹 후 값 | 이유 |
|---|---|---|
| `email` | `deleted_<userId>@deleted.local` | 이메일 주소는 개인식별정보(PII) |
| `displayName` | `Deleted User #<userId>` | 이름은 개인식별정보(PII) |
| `passwordHash` | `[DELETED]` | 인증 자격증명 무효화 |
| `createdAt` | 유지 | 감사 목적 (생성 시점) |
| `updatedAt` | 유지 | 감사 목적 (마지막 처리 시점) |
| `deletedAt` | `<삭제 시각 UTC>` | 삭제 이력 기록 필수 |

**유지되는 비식별 정보 (마스킹 대상 아님):**
- `id` (PK) — 연관 레코드 참조 유지
- `createdAt`, `updatedAt` — 감사 목적
- `deletedAt` — 삭제 시점 기록

#### 복구 가능 기간

| 상태 | 복구 가능 여부 | 복구 방법 |
|---|---|---|
| `DEACTIVATED` | **가능** | 사용자가 직접 로그인 후 복구 요청 |
| `SUSPENDED` | **가능** | 운영자 수동 해제 처리 |
| `DELETED` | **불가** | 마스킹 비가역적 — 개인정보 삭제 완료 상태 |

`DELETED` 상태 이후에는 어떤 방법으로도 원본 데이터 복구가 불가능하다. 감사 이벤트 로그만 운영 목적으로 접근 가능하다.

#### GDPR "Right to be Forgotten" 대응 계획

**현재 상태 (MVP):**
- Soft-Delete + 개인정보 마스킹으로 실질적 삭제 효과 달성
- 감사 로그에 마스킹 전/후 스냅샷 보존 (운영자 전용 접근)
- `deleted_<userId>@deleted.local` 형식의 식별자는 원본 PII를 포함하지 않음

**미래 과제 (별도 태스크):**
- EU GDPR 대응 시 Hard-Delete 기능 추가 (데이터 보존 기간 만료 후 레코드 물리 삭제)
- 개인정보 처리 방침 내 삭제 요청 처리 SLA 정의
- 데이터 보존 기간 정책 수립 (e.g., 삭제 후 N년 감사 로그만 유지)
- Right to Access / Right to Portability 엔드포인트 설계

#### 보안 경계 규칙

- **인증 차단:** `DEACTIVATED`, `SUSPENDED`, `DELETED` 사용자는 유효한 JWT가 있어도 요청 거부
- **API 응답:** DELETED 사용자를 조회 API에서 노출하지 않음 (조회 시 404 반환 원칙)
- **감사 로그 접근:** DELETED 사용자의 감사 이벤트는 운영자 전용 API로만 접근 가능

> 상세 상태 전이 규칙 및 마스킹 구현은 `docs/domain-rules.md § User Entity & Lifecycle` 참조

### Review Aggregation

Aggregation mode: **query-time** (not materialized projection). Source of truth: `reviews` + linked `occurrences` / `tours` / `instructor_profiles`.

| Endpoint | Aggregation Scope |
|---|---|
| `GET /tours/{tourId}/reviews/summary` | `TOUR` reviews under published tour occurrences |
| `GET /instructors/{instructorProfileId}/reviews/summary` | `INSTRUCTOR` reviews for active instructor under published occurrences |
| `GET /organizations/{organizationId}/reviews/summary` | `TOUR` + `INSTRUCTOR` under published occurrences, org-scoped |
| `GET /operator/organizations/{orgId}/reviews/summary` | All occurrence reviews for the org (operator scope) |

Summary payload fields: `count`, `averageRating`, `aggregationMode=QUERY_TIME`.

### Moderation — MVP No-Build

**Decision (2026-03-19): no-build.**

Rationale: Public UGC is limited to attendance-based reviews; no free-form community/post/comment surface exists. Allowing org operators to directly modify/hide review content creates trust surface distortion risk. Launch priority is review aggregation consistency, not a moderation console.

Current runtime and OpenAPI do not expose moderation endpoints. Emergency handling (legal issues, abuse, PII exposure) is handled via Jira + manual operator action.

Re-evaluate when: public community surface added; review appeal/report demand recurs; regulatory/legal audit-trail requirement at API level.

### Favorites UX Rules

- `GET /me/favorites` sorted `createdAt DESC`.
- Only published tours eligible for favoriting.
- Unpublished / deleted-equivalent tours excluded from list response.
- No filter MVP.

### Notifications UX Rules

- `GET /me/notifications` sorted `createdAt DESC`.
- Unread = `readAt == null`. No unread-count endpoint MVP. No cursor pagination MVP.
- Suppression: no notification for inquiry self-message; same resource/action/user → single notification; delivery retry in delivery log layer only (no duplicate read-model entries).

---

## 6. Cross-References

| Document | Content |
|---|---|
| `docs/domain-rules.md` | Domain invariants, state machines, eligibility rules |
| `docs/schema.md` | Entity definitions and FK/denormalization decisions |
| `docs/openapi.yaml` | HTTP contract, error code map, request/response schemas |
| `docs/architecture.md` | Hexagonal architecture, guard implementation patterns |
