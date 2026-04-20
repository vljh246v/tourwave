---
id: T-998
title: "[BE] infra — Status sync end-to-end smoke (throwaway)"
aliases: [T-998]

repo: tourwave
area: be
milestone: M1
domain: infra
layer: infra
size: XS
status: in-progress

depends_on: []
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-20
updated: 2026-04-20
---

#status/in-progress #area/be

# T-998 — [BE] infra — Status sync end-to-end smoke (throwaway)

> 이 카드는 sub-project #4 Task 19 e2e smoke 전용 throwaway 카드입니다. 작업 완료 후 삭제됩니다.

## SSOT 근거
- Sub-project #4 Task 19: task-start.sh + task-finish.sh status sync e2e smoke
- `scripts/task-status-sync.py` — status 패치 로직 검증 대상
- `scripts/validators/07-status-sync-smoke.sh` — smoke validator 검증 대상

## 구현 지침
1. `task-start.sh T-998-status-sync-e2e` 실행으로 status backlog → in-progress 자동 패치 확인
2. worktree 내 더미 변경 커밋
3. `task-finish.sh T-998-status-sync-e2e` 실행으로 status → done 패치 + merge + cleanup 확인
4. 카드 제거로 마무리

## Verification
```bash
./scripts/verify-task.sh T-998-status-sync-e2e
```
예상 결과: 07-status-sync-smoke ✓

## Notes
- 이 카드는 smoke 전용이며 실제 코드 변경을 포함하지 않습니다.
- 검증 완료 후 `chore(smoke): T-998 e2e smoke 카드 제거` 커밋으로 삭제됩니다.

<!-- smoke-change -->
