# OpenAPI Gap Report (Historical Archive)

이 문서는 특정 시점의 OpenAPI gap 분석 기록을 보관하는 아카이브다. 현재 상태 판단의 1차 기준으로 사용하지 않는다.

현재 truth 확인 순서:

1. 실제 controller 구현
2. 관련 integration / service test
3. `agent/13_api_status_matrix.md`
4. `agent/04_openapi.yaml`

## How To Use This File

- 이 문서는 "예전에 어떤 종류의 drift가 있었는지"를 기억하기 위한 기록이다.
- 현재 미구현/구현 여부를 여기서 확정하지 않는다.
- 새로운 API 작업에서는 `agent/09_spec_index.md`의 update order를 따른다.

## Historical Drift Themes

과거에 반복적으로 문제가 되었던 축은 아래와 같다.

- current runtime path와 target OpenAPI path가 달라지는 경우
  - 예: `refund-preview`, waitlist operator path
- 조회 endpoint가 status 문서에는 있지만 OpenAPI에는 늦게 반영되는 경우
  - 예: inquiry message list, roster export
- auth 설명이 target contract와 current runtime을 혼용하는 경우
  - 예: JWT target과 header actor context runtime이 동시에 존재하는 상태

## Closed Historical Examples

현재 코드 기준으로 이미 구현되어 있고 current truth 문서에도 반영된 예시:

- `POST /bookings/{bookingId}/complete`
- `POST /occurrences/{occurrenceId}/finish`
- `GET /bookings/{bookingId}/refund-preview`
- `POST /bookings/{bookingId}/waitlist/promote`
- `POST /bookings/{bookingId}/waitlist/skip`
- `GET /inquiries/{inquiryId}/messages`
- `GET /occurrences/{occurrenceId}/participants/roster/export`

## Current Open Items

2026-03-17 기준으로 남아 있는 핵심 gap은 "미구현 product surface" 쪽이다.

- auth/account/me target contract
- organization/member management contract
- instructor onboarding/profile contract
- tour/occurrence authoring and public catalog contract
- external payment webhook/callback contract

상세 우선순위는 아래 문서를 따른다.

- current truth: `agent/13_api_status_matrix.md`
- delivery plan: `agent/16_product_delivery_roadmap.md`
- next backlog: `agent/15_next_development_backlog.md`
