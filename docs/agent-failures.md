# Agent Failures — 에이전트 실패 로그

에이전트가 범한 실수와 그 구조적 대응을 기록합니다.

## 승격 정책

- 같은 패턴 **2회 반복** → `golden-principles.md`에 원칙 추가
- 같은 패턴 **3회 반복** → 린트/테스트로 **구조적 불가능화** 필수

## 실패 기록

| ID | 날짜 | 증상 | 근본 원인 | 강화책 | 도메인 |
|----|------|------|-----------|--------|--------|
| F-2026-04-26-1 | 2026-04-26 | T-001 orchestrator가 구현 완료 보고 후 task-finish.sh 1차 실행 시 빈 merge("Already up to date"), 워크트리 cleanup 실패. 실제로는 구현 파일이 모두 워크트리에 미커밋 상태로 잔존 | tdd-backend 서브에이전트가 Write로 파일 생성 후 git commit 누락. verify 검증기는 작업트리 파일을 직접 읽어 PASS, orchestrator/Phase 8도 워크트리 git status 미확인 | (1) task-finish.sh 시작 시 `git status --porcelain` 가드 추가 권고 — 미커밋 시 즉시 abort. (2) orchestrator.md Phase 8.5 "워크트리 git status 청결 확인" 단계 추가 검토. (3) 반복 시 golden-principles 승격 | 워크플로우 |
| F-2026-04-26-2 | 2026-04-26 | T-002 task-finish.sh 직전 점검 시 메인 repo에 `docs/audit/BE-auth.md` 미커밋 수정이 잔존. 워크트리 commit `69b5e36` 메시지에는 "M-1 BE-auth.md updated" 명시되어 있었으나 실제 커밋 stat에는 미포함 — orchestrator/서브에이전트가 워크트리 외부(메인 repo) 절대경로로 Edit 실행 | 서브에이전트 prompt에 "워크트리 절대경로 안에서만 작업"을 명시했음에도 일부 docs 수정이 메인 repo로 누출. F-2026-04-26-1과 같은 root cause 계열(워크트리 위생 위반)이며 같은 날 2회째 발생 | (1) **golden-principles GP-007 신규 등재**: 서브에이전트는 워크트리 외부 파일 수정 금지. (2) Phase 8 가드: 메인 repo `git status --porcelain`도 검사 (history.jsonl/exec-plans 외 변경 시 abort). (3) 3회 반복 시 hook으로 구조적 차단 | 워크플로우 |
| F-2026-04-26-3 | 2026-04-26 | T-904 task-finish.sh 1차 실행 시 빈 merge("Already up to date"), 워크트리 cleanup 실패. 23개 파일(구현 12 + 테스트 11)이 워크트리에 미커밋 잔존. F-2026-04-26-1과 동일 패턴 — tdd-backend 서브에이전트 2 라운드(보안 수정 포함) 모두에서 git commit 누락 | tdd-backend.md에 "각 stage 완료 후 commit" 절차 미명시. orchestrator Phase 8/9에서 워크트리 `git status --porcelain` 검사 미실행. F-2026-04-26-1 강화책(1) "task-finish.sh 가드 추가 권고"가 미실행 상태 | (1) **3회 도달 — 구조적 차단 의무화**: pre-tool-use hook으로 task-finish.sh 호출 직전 워크트리 git status 검사, 미커밋 파일 존재 시 abort. (2) tdd-backend.md 절차에 "각 stage Red-Green 후 commit" 명시. (3) task-finish.sh 시작부에 `git -C "$WORKTREE" status --porcelain` 가드 즉시 추가 | 워크플로우 |
| F-2026-04-27-1 | 2026-04-27 | T-006 완료 후 사후 점검 시: (1) tdd-backend가 카드 WRITE 경로(`adapter/in/web/user/MeControllerIntegrationTest.kt`)대로 파일 생성했으나, 실제 MeController는 `adapter/in/web/auth/`에 있어 auth/ 에도 동일 7케이스를 중복 생성 — user/ 잔재 미정리. verify-task.sh는 두 파일 모두 PASS하여 검출 불가. (2) `docs/exec-plans/completed/T-006.md`가 빈 템플릿(`[작업 목표를 작성하세요]`) 그대로 archived. (3) docs/audit/gap-matrix 갱신 없음 | (a) 카드 WRITE 경로 vs 실제 src 정합성 검증 단계 부재 — tdd-backend가 카드 경로를 맹목적으로 따름. (b) exec-plan 본문 작성 강제 단계 부재 — task-finish.sh가 active→completed 이동만 수행, 본문 검사 없음. (c) Phase 7.5가 SSOT(openapi/schema/policies/domain-rules)만 커버 — audit/gap-matrix/exec-plan은 미커버 | T-907 신설 3단계: (1) orchestrator Phase 0 sub-step(카드 WRITE 경로 사전 체크 + BLOCKER 의무화) (2) Phase 7.6(문서 동기화 강제 — exec-plan 본문/audit/gap-matrix/testing.md) (3) Phase 8.5(잔재·중복 자동 탐지). tdd 에이전트에 WRITE_PATH_MISMATCH BLOCKER 의무 추가. verification Step 7.5 신설 | 워크플로우 |

## 기록 방법

- **ID**: `F-YYYY-MM-DD-N`
- **증상**: 외부에서 관찰 가능한 현상
- **근본 원인**: 구조적 사유
- **강화책**: ADR, 린트, 검증기, 문서 링크
- **도메인**: 워크플로우 / 보안 / 아키텍처 / 테스트 / 문서 등
