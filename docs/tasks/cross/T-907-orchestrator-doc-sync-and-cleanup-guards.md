---
id: T-907
title: "T-907 — [Infra] orchestrator 문서·파일 정합성 가드 (Phase 0/7.6/8.5)"
aliases: [T-907]

repo: tourwave
area: be
milestone: cross
domain: infra
layer: tooling
size: L
status: done

depends_on: []
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-27
updated: 2026-04-27
---

#status/done #area/be

# T-907 — [Infra] orchestrator 문서·파일 정합성 가드

## 파일 소유권
WRITE:
  - `~/.claude/agents/orchestrator.md` (Phase 0/7.6/8.5 신설)
  - `~/.claude/agents/tdd-backend.md` (정리 의무 명시)
  - `~/.claude/agents/tdd-frontend.md` (동일)
  - `~/.claude/agents/verification.md` (정합성 체크 추가)
  - `scripts/verify-task.sh` (선택: 08-doc-consistency.sh 추가)
  - `scripts/validators/08-doc-consistency.sh` (신규, 선택)

READ:
  - `logs/trends/failure-patterns.md` 2026-04-27 항목
  - `docs/agent-failures.md` (가능하면 신규 F-ID 등재)
  - `docs/golden-principles.md`

DO NOT TOUCH:
  - 도메인 코드, 도메인 테스트

## SSOT 근거
- `logs/trends/failure-patterns.md` 2026-04-27 / T-006 항목: 작업 완료 후 문서·정합성 누락 패턴
- `docs/agent-failures.md` F-2026-04-26-1/2/3 (관련 패턴, 워크트리 정합성 결함과 동일 계열)

## 현재 상태 (갭)

### 결함 A — Phase 7.5 SSOT sync 범위 협소
- `~/.claude/agents/orchestrator.md` Phase 7.5는 openapi/schema/policies/domain-rules만 propagate.
- `docs/audit/BE-<domain>.md`, `docs/gap-matrix.md`, `docs/testing.md` "미커버 항목" 변동은 검증/갱신 안 됨.
- 결과: 구현으로 갭이 닫혀도 audit/gap-matrix가 stale → 다음 작업자가 불필요 재작업.

### 결함 B — exec-plan 본문 누락
- `task-start.sh`가 `docs/exec-plans/active/<TASK_ID>.md`를 빈 템플릿으로 생성.
- `task-finish.sh`는 active→completed로 이동만 하고 본문 작성 강제 안 함.
- T-006: completed/T-006.md가 "[작업 목표를 작성하세요]" 그대로 archived. 회고/결정 기록 0.

### 결함 C — 카드 WRITE 경로 vs 실제 src 정합성 부재
- 카드의 WRITE 경로는 작성 시점 추정. 실제 코드 위치와 다를 수 있음.
- T-006 카드 WRITE: `adapter/in/web/user/MeControllerIntegrationTest.kt` ↔ 실제 MeController: `adapter/in/web/auth/`.
- tdd 에이전트가 카드 따라 만든 뒤 실제 위치에 또 만들어 중복 생성. 정리 단계 없음.
- verify-task.sh는 두 파일 모두 PASS → 검출 못함.

## 구현 지침

### 1. Phase 0 추가 — 카드 정합성 사전 체크 (orchestrator.md)
TASK_ID가 `docs/tasks/`에 매치되는 경우, Phase 0.5 직전에 다음 체크:

```
- 카드 WRITE 경로 각 항목에 대해:
  - 파일이 존재하면: src 패키지/위치가 카드의 layer 필드와 정합하는지 검증
  - 파일이 신규면: 부모 디렉터리에 동일 책임의 기존 파일이 있는지 grep
  - 불일치/모호 시 사용자에게 사전 보고 후 진행
```

### 2. Phase 7.6 신설 — 문서 동기화 (orchestrator.md, Phase 7.5 직후)
다음 5단계 강제:

1. **exec-plan 본문 작성**: `docs/exec-plans/active/<TASK_ID>.md`의 빈 템플릿 섹션을 다음으로 채움:
   - 배경 및 목표 (카드 SSOT 근거 기반 1~3줄)
   - 구현 단계 (실제 stage commit 시퀀스)
   - 영향 범위 (실제 변경 파일 목록 — `git diff --stat` 기반)
   - 결정 기록 (escalation/포기/대안 채택 시)
   - 잔재/후속 (분할된 후속 카드 ID)
2. **audit 갱신 체크**: 작업 도메인의 `docs/audit/BE-<domain>.md`에 커버리지 변동이 있으면 갱신. 없으면 "변동 없음" 명시.
3. **gap-matrix 항목 close**: `docs/gap-matrix.md`에서 본 카드의 SSOT 근거 항목 status를 갱신 (예: 🟡 → 🟢, 또는 항목 제거).
4. **testing.md 미커버 항목**: 새 테스트가 추가되었으면 "미커버 항목" 섹션에서 해당 라인 제거.
5. **모든 변경은 워크트리 안에서 commit** — 메인 repo 직접 수정 금지 (GP-007).

체크리스트는 verification 에이전트가 검증.

### 3. Phase 8.5 신설 — 잔재/중복 cleanup (orchestrator.md, Phase 8 직후)

```
verification PASS 후 다음 자동 탐지:

(a) 카드 WRITE 경로의 파일이 동일 이름으로 다른 디렉터리에도 존재하는지:
    find src -name "<filename>" → 2개 이상이면 사용자에게 보고
(b) `git diff develop...HEAD --name-only`의 *Test.kt 파일에 대해 클래스명/케이스
    중복 검사 (jdeps 또는 단순 grep -c '@Test' 비교)
(c) src 측 변경 파일이 카드 WRITE에 명시되지 않은 경로면 사용자에 보고
```

자동 삭제 금지. 사용자 승인 후 정리 commit.

### 4. tdd-backend.md / tdd-frontend.md 의무 추가
"카드 WRITE 경로 따른 뒤, 실제 src 컨트롤러/서비스 패키지와 다르면 카드 따르지 말고 orchestrator에 보고. 절대 두 위치에 동시 생성 금지."

### 5. verification.md 체크리스트 보강
verify-task.sh PASS 외에 다음 명시 점검:
- exec-plan 본문 채워짐 (`grep -q '\[작업 목표를 작성하세요\]'` ⇒ FAIL)
- audit/gap-matrix/testing.md 갱신 여부 (작업 도메인 한정)
- 워크트리 커밋의 stat이 실제 src 변경과 일치 (워크트리 외부 수정 누출 여부 — F-2026-04-26-2 재발 방지)

### 6. 선택 — `scripts/validators/08-doc-consistency.sh` (구조적 자동화)
- exec-plan 빈 템플릿 검출
- 동일 클래스 이름 테스트 파일 중복 검출
- exit 1 시 verify-task.sh FAIL

위 5번을 사람/에이전트 의존 없이 자동화. 다만 false positive 위험 — 옵션으로 도입.

## Acceptance Criteria
- [ ] orchestrator.md Phase 0/7.6/8.5 신설
- [ ] tdd-backend.md / tdd-frontend.md "카드 WRITE 경로 vs 실제 패키지 불일치 보고 의무" 추가
- [ ] verification.md 체크리스트에 exec-plan 본문/audit/gap-matrix 항목 추가
- [ ] (선택) `scripts/validators/08-doc-consistency.sh` 도입
- [ ] T-006 case study가 `docs/agent-failures.md`에 F-ID로 등재
- [ ] 본 카드 작업 자체가 새 가드를 따라 진행됨 (본 카드 exec-plan 본문이 풍부히 작성, gap-matrix 갱신 등 — 셀프 dogfooding)

## Verification
`./scripts/verify-task.sh T-907`
- (선택 항목 도입 시) 08-doc-consistency.sh 실행 및 통과
- agents/*.md 변경에 대한 lint 없음 → 사람 리뷰 + 다음 /dev 1회 실측

## Rollback
agents/*.md는 git revert. validators/08-doc-consistency.sh 삭제.

## Notes
- 본 가드의 효과는 다음 신규 카드(/dev) 1회 실측 후 retrospective 작성으로 검증.
- failure-patterns.md 2026-04-27 / T-006 항목과 직접 연결.
- F-2026-04-26-2 (워크트리 외부 수정 누출)와 본 결함은 다른 계열이지만 verification 보강으로 동시 커버 가능.
- Phase 8.5 자동 탐지의 false positive(예: 정당한 동일 클래스명 중복 = 의도된 디자인) 가능성 — 자동 삭제 금지, 보고만.
- 분할 고려: 결함 A/B/C가 독립 → 3개 sub-task로 쪼갤 수 있음. 단 orchestrator.md 수정이 공통이라 한 카드 권장.
