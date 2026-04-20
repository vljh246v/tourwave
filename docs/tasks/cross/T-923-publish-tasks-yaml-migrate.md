---
id: T-923
title: "[BE] infra — publish-tasks.sh YAML frontmatter 포맷 재작성"
aliases: [T-923]

repo: tourwave
area: be
milestone: cross
domain: infra
layer: infra
size: M
status: backlog

depends_on: []
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-20
updated: 2026-04-20
---

#status/backlog #area/be

# T-923 — [BE] infra — publish-tasks.sh YAML 재작성

## 파일 소유권
WRITE:
  - `scripts/publish-tasks.sh`

READ:
  - `scripts/validate-tasks.sh` (awk YAML 파싱 패턴 참고)
  - `scripts/blocker-rank.sh` (is_done / has_dependency 유틸 참고)
  - `docs/tasks/**/T-*.md` (신 YAML 포맷 샘플)

DO NOT TOUCH:
  - 태스크 카드 본문

## SSOT 근거
- Sub-project #4 spec Future Work FW-1 — `_specs/2026-04-20-status-auto-sync-design.md`
- 2026-04-20 YAML frontmatter 마이그레이션 이후 publish-tasks.sh 의 `- Meta:` 파싱 로직 고장 → GitHub Issue 발행 불가

## 현재 상태 (갭)
- [ ] `extract_meta()` 가 `- Milestone:` 같은 OLD 리스트 포맷만 파싱
- [ ] 카드 frontmatter 의 `milestone`/`area`/`domain`/`size` 를 읽지 못함
- [ ] `grep "^- GitHub Issue: #"` 도 실패 (새 포맷은 `github_issue:` YAML 필드)

## 구현 지침
1. awk 기반 frontmatter 파서 유틸 함수 추출 (validate-tasks.sh 의 `has_frontmatter_field` 패턴 재사용)
2. `extract_meta` 를 frontmatter field lookup 으로 교체
3. "이미 발행됨" 체크를 `github_issue: null` 감지로 변경
4. 발행 후 업데이트는 YAML 필드 `github_issue:` 에 숫자 할당 (sed in-place)
5. 레이블 조합 규칙 유지
6. `--dry-run` 흐름 동일

## Acceptance Criteria
- [ ] `./scripts/publish-tasks.sh all --dry-run` 이 40 카드 전체 인식
- [ ] 단일 카드 실제 발행 smoke (임시 카드 생성 → 발행 → github_issue 필드 세팅 → 카드 제거)
- [ ] 이미 발행된 카드 skip 로직 정상

## Verification
`./scripts/publish-tasks.sh all --dry-run`

## Rollback
`git revert <commit>`

## Notes
- FW-1 에서 승격된 작업
