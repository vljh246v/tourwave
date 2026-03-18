# Next Development Backlog

이 문서는 Sprint 19 완료 이후 기준으로 "현재 제품 수준까지 남은 작업"을 handoff 용도로 정리한다. 구현 완료 구조는 `16_product_delivery_roadmap.md`, 남은 gap closure의 상세 sprint/epic/story/subtask 구조는 `17_release_gap_execution_plan.md`를 따른다.

## 1. Priority Order

### P0

- alert routing / dashboard / SLO baseline
- real MySQL container suite 확대
- operator DLQ / remediation queue hardening

### P1

- `POST /me/delete`
- backup / incident contact / rollback evidence hardening

### P2

- Gradle 멀티모듈 분리
- future moderation 재검토 조건 정의 유지

## 2. Recommended Next Sprint Themes

### Theme A. Launch Operations

- alerting / dashboard / SLO
- real MySQL suite expansion
- dead-letter / remediation queue

## 3. Immediate Actionable Tasks

1. alert rule / dashboard / SLO baseline을 문서와 actuator metric 기준으로 닫는다.
2. real MySQL suite를 smoke 이상 회귀로 확장하고 flaky policy를 고정한다.
3. payment/notification/webhook 실패 projection을 operator remediation queue로 묶는다.
4. `POST /me/delete`의 제품 필요성을 출시 범위에서 재확인한다.
5. 새 구현 티켓은 `17_release_gap_execution_plan.md`의 sprint/stage 구조에 맞춰 생성한다.

## 4. Rules For Future Tickets

- 상위 티켓을 만들기 전에 구현 surface를 `13_api_status_matrix.md`와 대조한다.
- 서브티켓은 `schema`, `application`, `adapter`, `test/docs` 축으로 쪼갠다.
- 기능 완료 후에는 테스트 추가와 `./gradlew test` 확인을 함께 완료 기준으로 둔다.
