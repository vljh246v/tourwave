# Spec Index & Governance (MVP)

이 문서는 `agent` 스펙 문서의 **읽는 순서, 우선순위, 충돌 해결 규칙**을 정의한다.

---

## 1) Source of Truth 우선순위

구현/리뷰 시 아래 우선순위를 따른다.

1. `01_domain_rules.md`
2. `10_architecture_hexagonal.md`
3. `08_operational_policy_tables.md`
4. `04_openapi.yaml`
5. `05_authz_model.md`
6. `02_schema_mysql.md`
7. `06_implementation_notes.md`
8. `07_test_scenarios.md`
9. `03_api_catalog.md`
10. `00_overview.md`

충돌 시 상위 문서가 하위 문서를 override 한다.

---

## 2) 권장 읽기 순서

신규 개발자 온보딩 기준:

1. `00_overview.md` (도메인 배경)
2. `09_spec_index.md` (거버넌스)
3. `01_domain_rules.md` (핵심 규칙)
4. `10_architecture_hexagonal.md` (아키텍처 강제 규칙)
5. `08_operational_policy_tables.md` (정책 매트릭스)
6. `04_openapi.yaml` (API 계약)
7. `05_authz_model.md` (권한 모델)
8. `02_schema_mysql.md` (저장 모델)
9. `06_implementation_notes.md` (가드레일)
10. `07_test_scenarios.md` (검증 시나리오)
11. `03_api_catalog.md` (빠른 레퍼런스)

---

## 3) 핵심 합의 요약

- Booking 생성 상태 결정:
  - seats available -> `REQUESTED`
  - seats 부족 -> `WAITLISTED`
- Inquiry 생성:
  - `bookingId` 필수
  - occurrence/org/booking 스코프 일치 필수
- Attendance:
  - booking 단위가 아니라 participant 단위
- Invitation 만료:
  - participant 상태에만 영향, booking 상태는 유지
- Offer 만료:
  - `OFFER_EXPIRED`는 waitlist offer 문맥으로 한정

---

## 4) Mutation Safety 표준

- 도메인 상태 변경 endpoint는 `Idempotency-Key` 필수
- 동일 key + 동일 payload 재요청은 최초 응답 재반환
- 동일 key + 다른 payload는 `422 IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD`
- 처리 중 중복요청은 `409 IDEMPOTENCY_IN_PROGRESS` 허용

대표 대상:

- `POST /occurrences/{occurrenceId}/bookings`
- `POST /bookings/{bookingId}/cancel`
- `POST /bookings/{bookingId}/offer/accept`
- `POST /bookings/{bookingId}/offer/decline`
- `PATCH /bookings/{bookingId}/party-size`
- `POST /occurrences/{occurrenceId}/inquiries`

---

## 5) 에러 코드 운영 원칙

- 공통 에러 포맷: `ErrorResponse`
- 에러 코드 사전: `components.schemas.ErrorCode`
- 정책 매핑: `x-error-code-map`
- 재사용 예시 payload: `components.examples`

검증 규칙:

- 카탈로그(`03`)와 OpenAPI(`04`)의 대표 에러코드 표가 일치해야 함
- 신규 write endpoint 추가 시 409/422 대표 example 최소 1개 이상 추가

---

## 6) 데이터/운영 강제 항목

- `inquiries.booking_id`는 `NOT NULL`
- append-only `audit_events` 기록 필수
- 결제/환불 시도는 `payment_transactions`로 추적
- `REFUND_PENDING` 상태 재시도 job 운영
- waitlist/offer/seat 변경 트랜잭션에서 occurrence row lock 필수

---

## 7) 변경 관리 체크리스트

스펙 변경 PR은 아래를 모두 충족해야 한다.

- [ ] `01` 또는 `08`에 도메인 규칙/정책 변경 반영
- [ ] `10` 아키텍처 규칙(의존 방향/패키지 경계) 준수 검토
- [ ] `04` OpenAPI 계약 및 examples 반영
- [ ] `05` 권한 영향 검토 및 반영
- [ ] `02` 스키마 영향 검토 및 반영
- [ ] `06` 구현 가드레일 반영
- [ ] `07` 테스트 시나리오 추가/수정
- [ ] `03` 카탈로그 요약 업데이트

추가 검증 규칙:

- [ ] 기능 구현 후 기존 코드 헥사고날 리팩터 단계(06 문서 Step A~D) 실행 여부 확인

---

## 8) 빠른 점검 커맨드

```bash
# OpenAPI YAML 파싱 검증
python3 -c "import yaml; yaml.safe_load(open('agent/04_openapi.yaml')); print('ok')"

# 핵심 키워드 정합성 확인
rg -n "Idempotency-Key|BOOKING_SCOPE_MISMATCH|OFFER_EXPIRED|REFUND_PENDING|audit_events" agent
```
