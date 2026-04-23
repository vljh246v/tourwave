---
id: T-997
title: "[cross] Cross-repo doc sync (#5)"
aliases: [T-997]

repo: cross
area: cross
milestone: cross
domain: infra
layer: infra
size: M
status: in-progress

depends_on: []
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

ssot_refs: []
reviewed: ""
stale_refs: []

created: 2026-04-20
updated: 2026-04-23
---

#status/in-progress #area/cross

# T-997 — Cross-repo doc sync (Sub-project #5)

BE SSOT 문서 (openapi/schema/policies/domain-rules) 또는 상류 태스크가 변경됐을 때,
전제로 삼은 카드의 `stale_refs:` 필드에 드리프트 엔트리를 자동 추가.
git-persistent 플래그로 baseline invalidation 표현.

상세는 exec-plan 참조: `docs/exec-plans/active/T-997-cross-repo-doc-sync.md`.

## 파일 소유권
WRITE:
  - `scripts/task-status-sync.py` (compute_stale_refs 호출, stale_refs 필드 관리)
  - `scripts/git-hooks/post-commit-stale-refs.sh` (신규, git-persistent 플래그)

READ:
  - BE `docs/tasks/**/*.md` (kard 프론트매터)
  - FE `docs/tasks/**/*.md` (크로스 repo 참조)
  - `docs/domain-rules.md`, `docs/openapi.yaml` (SSOT 기준)

## SSOT 근거
- Sub-project #5 spec — `_specs/2026-04-20-cross-repo-doc-sync-design.md`
- bootstrap plan — `_plans/2026-04-20-cross-repo-doc-sync-bootstrap.md`
- Phase 4 Task 14 — vault Dataview `_index/stale.md` 의존

## 현재 상태 (갭)
- [x] compute_stale_refs 순수 알고리즘 (Phase 1 Task 5)
- [x] task-status-sync.py reviewed/propagate CLI (Phase 2 Tasks 6-7)
- [x] validate-tasks.sh 확장 (Phase 3 Tasks 8-9)
- [x] 08-propagate-smoke.sh 파이프라인 (Phase 3 Tasks 10-11)
- [x] task-finish.sh 훅 (Phase 3 Task 12)
- [x] 템플릿 3개 + migrate-tasks.py coerce (Phase 4 Task 13)
- [x] vault index Dataview (Phase 4 Task 14)
- [x] E2E 실전 시뮬레이션 (Phase 5 Task 15)
- [x] 최종 문서 업데이트 + task-finish (Phase 5 Task 16)

## 구현 지침
1. 17개 시뮬레이션 카드 CRUD 완성 (08-propagate-smoke.sh)
2. task-status-sync.py 메인 로직 검증
3. git-persistent 플래그 동작 검증
4. 전체 주기 엔드-투-엔드 테스트

## Acceptance Criteria
- [x] 17개 시뮬레이션 카드 전부 CRUD 완료
- [x] task-status-sync.py 모든 CLI 정상 동작
- [x] verify-task.sh 통과 (검증기 7/7)
- [x] task-finish.sh 병합 및 status → done 자동 전이

## Verification
`./scripts/verify-task.sh T-997`
- compute_stale_refs 동작 검증
- migrate-tasks.py coerce 검증
- 08-propagate-smoke.sh 시뮬레이션 카드 CRUD 검증
- task-status-sync.py 전체 CLI 검증
- git-persistent 플래그 검증

## Rollback
`git revert <commit>`

## Notes
- 17개 시뮬레이션 카드는 task-finish.sh에서 자동 롤백됨 (cleanup)
- cross-repo doc sync는 BE/FE 공동 협력 — FE 카드는 BE 완료 후 순차 진행
- SSOT 다중 소스 (openapi.yaml, domain-rules.md, policies.md 등) 모두 감시
