# Next Development Backlog

이 문서는 Sprint 14 완료 이후 기준으로 "현재 제품 수준까지 남은 작업"을 handoff 용도로 정리한다. Sprint 14까지의 구현 완료 구조는 `16_product_delivery_roadmap.md`, 남은 gap closure의 상세 sprint/epic/story/subtask 구조는 `17_release_gap_execution_plan.md`를 따른다.

## 1. Priority Order

### P0

- Spring Security perimeter enforcement
- email verification / password reset / account deactivation
- real asset storage adapter
- real payment provider adapter
- outbound notification delivery
- alert routing / dashboard / SLO baseline

### P1

- public review aggregation by tour / instructor
- announcements / operator communication surface
- organization report APIs
- real MySQL container suite 확대
- operator DLQ / remediation queue hardening

### P2

- moderation 정책 확정 및 필요 시 API 도입
- favorites / notifications 고도화
- Gradle 멀티모듈 분리

## 2. Recommended Next Sprint Themes

### Theme A. Security And Account Closure

- route-level auth enforcement
- email verify / password reset
- account lifecycle

### Theme B. Real Integrations

- asset storage
- payment provider
- notification delivery

### Theme C. Launch Operations

- alerting / dashboard / SLO
- report surfaces
- real MySQL suite expansion

## 3. Immediate Actionable Tasks

1. `SecurityConfig` 기준으로 보호 경로와 공개 경로를 재정의한다.
2. email verification / password reset / account deactivate contract를 current gap 문서와 맞춘다.
3. fake/stub integration을 real adapter boundary와 rollout 문서로 분리한다.
4. launch 차단 운영 항목을 alert/dashboard/runbook 기준으로 다시 정의한다.
5. 새 구현 티켓은 `17_release_gap_execution_plan.md`의 sprint/stage 구조에 맞춰 생성한다.

## 4. Rules For Future Tickets

- 상위 티켓을 만들기 전에 구현 surface를 `13_api_status_matrix.md`와 대조한다.
- 서브티켓은 `schema`, `application`, `adapter`, `test/docs` 축으로 쪼갠다.
- 기능 완료 후에는 테스트 추가와 `./gradlew test` 확인을 함께 완료 기준으로 둔다.
