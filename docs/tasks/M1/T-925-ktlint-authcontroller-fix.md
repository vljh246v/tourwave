---
id: T-925
title: "[BE] infra — AuthController ktlint multiline 수정"
aliases: [T-925]

repo: tourwave
area: be
milestone: M1
domain: auth
layer: adapter.in
size: XS
status: done

depends_on: []
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-20
updated: 2026-04-20
---

#status/done #area/be

# T-925 — [BE] infra — AuthController ktlint multiline 수정

## 파일 소유권
WRITE:
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/auth/AuthController.kt`

READ:
  - (없음)

DO NOT TOUCH:
  - 다른 레이어 파일

## SSOT 근거
- Sub-project #4 Task 19 Fallback 경로 원인 — `_session/resume-2026-04-20.md` 오후 4차 C 항목
- ktlint `standard:multiline-expression-wrapping` 규칙: `val x = ResponseCookie.from(...)` 같은 multiline chain 은 `val x =` 다음 줄부터 시작해야 함
- 현재 `task-finish.sh` 를 BLOCK 중 → Sub-project #4 하네스 첫 실전 투입 (파이프라인 스모크 겸)

## 현재 상태 (갭)
- [ ] AuthController.kt:66:27 `val clearAccess = ResponseCookie.from(...)` multiline 시작 위치 오류
- [ ] AuthController.kt:73:28 `val clearRefresh = ResponseCookie.from(...)` 동일 이슈

## 구현 지침
1. `val clearAccess =` 와 `val clearRefresh =` 우측 `ResponseCookie.from(...)` 체인을 새 줄로 시작
2. 체인 indentation 을 8-space 로 (기존 4-space → 8-space)
3. 의미 변경 없음 — 순수 스타일 수정

## Acceptance Criteria
- [ ] `./gradlew ktlintCheck` PASS
- [ ] `./scripts/verify-task.sh` 5 validator PASS
- [ ] `./scripts/task-finish.sh` direct 모드 정상 머지

## Verification
`./scripts/verify-task.sh T-925-ktlint-authcontroller-fix`

## Rollback
`git revert <merge-commit>`

## Notes
- Sub-project #4 develop-first 하네스의 첫 실전 실행
- 성공 시 status → done 자동 전환 확인
