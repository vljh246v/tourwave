# OpenAPI Gap Report (Phase 1)

## 기준
- Source of Truth 우선순위: `agent/09_spec_index.md` (01 > 10 > 08 > 04 > 05 > 02 > 06 > 07 > 03 > 00)
- 대조 범위: 구현 컨트롤러(`src/main/kotlin/com/demo/tourwave/adapter/in/web/*`) vs `agent/04_openapi.yaml`의 booking/inquiry/review/occurrence 연산
- 집계 결과: OpenAPI 47개 연산 중 구현 15개, 미구현 33개, 구현만 존재 1개

## Critical mismatch (배포/구현 차단)
1. `POST /bookings/{bookingId}/complete` 미구현
   - 근거: `agent/04_openapi.yaml` + `agent/01_domain_rules.md`의 완료 상태 전이
   - 영향: `COMPLETED` 전이가 없어 리뷰 작성 자격(`ATTENDANCE_NOT_ELIGIBLE`) 경로가 우회됨
   - 제안: booking mutation에 COMPLETE 추가, idempotency/409/422/error.code 회귀 테스트 포함

2. `POST /occurrences/{occurrenceId}/finish` 미구현
   - 근거: `agent/04_openapi.yaml`, `agent/01_domain_rules.md`(Occurrence lifecycle)
   - 영향: Occurrence 종료 상태(`FINISHED`) 전이가 API 계약에서 누락됨
   - 제안: occurrence finish command 추가, audit/idempotency 정책 적용

3. `FINISHED` occurrence에 대한 신규 booking 생성 차단 누락
   - 근거: `agent/01_domain_rules.md`(After occurrence is FINISHED, new booking creation is not allowed)
   - 현재: `createBooking`는 `CANCELED`만 차단
   - 제안: `createBooking`에서 `FINISHED` 상태를 409 `INVALID_STATE_TRANSITION`으로 차단

## Major drift (구현 오해 유발)
1. 조회 계약 누락
   - `GET /inquiries/{inquiryId}`
   - `GET /reviews/{reviewId}`
   - `GET /bookings/{bookingId}`

2. 운영/관리 계약 누락
   - `PUT /bookings/{bookingId}/attendance`
   - `POST /bookings/{bookingId}/extend-offer`
   - `POST /bookings/{bookingId}/force-expire`
   - `POST /occurrences/{occurrenceId}/notify`
   - `GET|PUT /occurrences/{occurrenceId}/policies`

3. OpenAPI 미기재 구현
   - 구현 존재: `GET /occurrences/{occurrenceId}/reviews/summary`
   - OpenAPI에는 해당 operation 부재

## Minor gap (추후 보완)
- 검색/리포트/캘린더성 API 다수 미구현
  - `GET /search/occurrences`
  - `GET /organizations/{orgId}/reports/occurrences`
  - `GET /organizations/{orgId}/reports/bookings`
  - `GET /bookings/{bookingId}/calendar.ics`
  - `GET /occurrences/{occurrenceId}/calendar.ics`

## Phase 2 대상 선정 (High-impact)
- `POST /bookings/{bookingId}/complete`
- `POST /occurrences/{occurrenceId}/finish`

선정 사유:
- 도메인 상태 전이 핵심 경로(Booking 완료, Occurrence 종료)
- 리뷰/정산/운영 플로우의 선행 조건
- idempotency/409/422 정책 회귀 가치가 큼

## Phase 2 반영 결과
- 구현 완료:
  - `POST /bookings/{bookingId}/complete`
  - `POST /occurrences/{occurrenceId}/finish`
- 도메인 정합 보강:
  - `FINISHED` occurrence에서 `POST /occurrences/{occurrenceId}/bookings` 차단(409 `INVALID_STATE_TRANSITION`)
- OpenAPI 동기화:
  - 위 2개 endpoint에 `Idempotency-Key` 파라미터 및 409/422 예시 응답 반영
- 갭 수치 변화(대상 범위):
  - 미구현 33개 -> 31개
