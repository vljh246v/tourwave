---
id: T-924
title: "[BE] infra — /blockers skill 문서 YAML frontmatter 재작성"
aliases: [T-924]

repo: tourwave
area: be
milestone: cross
domain: infra
layer: infra
size: S
status: done

depends_on: []
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-20
updated: 2026-04-26
---

#status/done #area/be

# T-924 — [BE] infra — /blockers skill YAML 재작성

## 파일 소유권
WRITE:
  - `.claude/skills/blockers/SKILL.md`

READ:
  - `scripts/blocker-rank.sh` (YAML 파싱 이미 적용된 패턴)
  - `docs/tasks/**/T-*.md` (신 YAML 포맷)

## SSOT 근거
- Sub-project #4 spec Future Work FW-2
- `/blockers` 스킬 문서가 "- ID:", "- Depends on:" 같은 OLD 리스트 포맷 참조
- `blocker-rank.sh` 스크립트는 이미 YAML 호환 (commit `8bc6136`) → 스킬 문서만 남음

## 현재 상태 (갭)
- [ ] SKILL.md "전제" 섹션이 `- Meta` 섹션 기대
- [ ] 조회 커맨드 예시가 OLD 포맷 전제

## 구현 지침
1. "전제" 섹션 — 카드 구조 설명을 YAML frontmatter 기준으로 재작성
2. Step 2 조회 커맨드 예시 업데이트
3. frontmatter 필드 명 (id / depends_on / blocks / milestone / area) 명시
4. 스킬 동작 자체는 `blocker-rank.sh` 호출이므로 로직 변경 없음 — **문서만** 수정

## Acceptance Criteria
- [ ] 스킬 문서에 OLD 포맷 예시 (`- ID:`, `- Depends on:`) 0건
- [ ] `/blockers T-010` 사용자 호출 시 정상 결과 (수동 검증)

## Verification
수동: 사용자가 `/blockers` 또는 `/blockers T-010` 호출 → 정상 응답 확인

## Rollback
`git revert <commit>`

## Notes
- FW-2 에서 승격된 작업
- 이미 `blocker-rank.sh` 가 YAML 지원 → 실제 동작은 정상일 것. 문서 drift 만 수정.
