---
description: Multi-repo 풀스택 TDD 개발을 orchestrator 에이전트에게 위임. tourwave(BE) + tourwave-web(FE) 양쪽을 조율하며 Contract-First OpenAPI 핸드셰이크, TDD, 보안 검증을 자동으로 수행.
argument-hint: <task-id> <한 줄 설명>
---

# /dev — Multi-repo 풀스택 개발 오케스트레이터

사용자가 입력한 작업을 `orchestrator` 서브에이전트에게 위임한다. orchestrator가 전체 파이프라인을 관리하며, 필요에 따라 `tdd-backend`, `tdd-frontend`, `security-reviewer`, `verification` 서브에이전트를 dispatch한다.

## 사용법

```
/dev <task-id> [scope-marker] <한 줄 설명>
```

예:
```
/dev T-042 예약 취소 API와 화면 추가
/dev WEB-101 예약 카드 컴포넌트 스타일 통일
/dev TASK-20260423 [BE-only] occurrence 타임존 표시 버그 수정
/dev T-050 [FS] 예약 환불 정책 신규
```

입력:
- `$1` (task-id): Jira 티켓 ID 또는 자동 생성 `TASK-YYYYMMDD-HHMMSS`
- `[scope-marker]` (선택): `[BE-only]` / `[FE-only]` / `[FS]` / `[Fullstack]`. 명시 시 orchestrator가 분류 LLM 추론을 스킵하고 즉시 채택.
- `$2...` (설명): 한국어 한 줄 요약

task-id가 없으면 orchestrator가 타임스탬프로 자동 생성한다.

**자동 분류 규칙 (마커 미명시 시):**
- task-id 접두어 `WEB-*` → FE-only
- task-id 접두어 `BE-*` → BE-only
- 그 외 → DESCRIPTION 내용 기반 LLM 분류

---

## 실행 절차

사용자 입력 전체를 다음 인자로 파싱한다:
- `TASK_ID` = 첫 번째 토큰 (형태 예: `T-###`, `PROJ-###`, `WEB-###`, `TASK-YYYYMMDD-HHMMSS`)
- `SCOPE_HINT` = 두 번째 토큰이 `[BE-only]`/`[FE-only]`/`[FS]`/`[Fullstack]` 중 하나면 해당 값, 아니면 빈 값
- `DESCRIPTION` = 나머지 전체

task-id 패턴이 보이지 않으면:
- Bash로 `date -u +%Y%m%d-%H%M%S` 실행하여 `TASK-<ts>`로 생성
- 사용자에게 "자동 생성된 ID: TASK-...로 진행합니다" 통지

파싱 후 **orchestrator 서브에이전트를 Agent 툴로 dispatch**한다. 메인 Claude는 직접 구현하지 않는다.

### Dispatch 템플릿

```
Agent({
  description: "Orchestrate dev for <TASK_ID>",
  subagent_type: "orchestrator",
  model: "sonnet",
  prompt: `
  [ROLE]
  당신은 orchestrator 에이전트다. 당신의 에이전트 정의(.claude/agents/orchestrator.md)를 읽고 지시대로 수행하라.

  [TASK_ID]
  ${TASK_ID}

  [SCOPE_HINT]
  ${SCOPE_HINT}  # [BE-only] / [FE-only] / [FS] / 빈 값
  명시되어 있으면 Phase 0 분류 LLM 추론 스킵하고 즉시 채택.

  [DESCRIPTION]
  ${DESCRIPTION}

  [DETAIL]
  (사용자가 이어서 추가 컨텍스트를 줬다면 여기에. 없으면 "추가 상세 없음")

  [MODE]
  자동 진행 모드. 다음 경우에만 사용자에게 묻는다:
  - 고위험 변경(docs/escalation-policy.md 해당)
  - Contract-First 3라운드 내 합의 실패
  - 보안 이슈 CRITICAL 발견
  - 최종 Phase 9 요약 보고 (task-finish.sh 실행은 사용자 승인 필요)

  [MODEL_SELECTION]
  당신은 sonnet으로 dispatch됨. 다음 시점에서만 self-escalate(opus 재dispatch 사용자에게 요청):
  - Phase 4 합의 충돌 시 중간 지점 결정
  - Phase 6 무한 루프 판정/승격
  - Phase 7 보안 이슈 심각도 재평가
  서브에이전트 모델은 orchestrator.md "모델 override 규칙" 표대로 dispatch마다 명시:
  - tdd-backend / tdd-frontend: 변경 복잡도에 따라 haiku/sonnet
  - security-reviewer / verification: 변경 파일 수 + escalation 여부에 따라 haiku/sonnet/opus

  [WORKTREE_RULES]
  각 repo의 ./scripts/task-start.sh <TASK_ID>로 워크트리 생성. 모든 서브에이전트는 워크트리 절대경로 내부에서만 작업.

  [TOKEN_RULES]
  - dialog.md 내용을 prompt에 인라인 금지. path만 전달.
  - status.json도 인라인 금지.
  - 추가 문서는 [REQUIRED_DOCS]로 명시한 것만 서브에이전트가 로드.

  [START]
  orchestrator.md의 Phase 0부터 차례로 실행하라. 각 Phase 결과를 ~/.claude/orchestrator-sessions/${TASK_ID}/dialog.md에 기록하라.
  `
})
```

### 진행 중 사용자 보고

orchestrator가 다음 시점에 사용자에게 보고할 수 있다:
- Phase 0.5: SSOT drift가 관련 영역에 발견된 경우
- Phase 1: 고위험 변경 감지 → 사용자 승인 대기
- Phase 4: Contract 합의 실패 → 사용자 결정
- Phase 7 security: CRITICAL 이슈 발견
- Phase 9: 최종 요약 → task-finish.sh 실행 여부

orchestrator가 질문하면 메인 Claude는 그대로 사용자에게 전달, 응답받아 orchestrator에게 릴레이.

### 종료 조건

orchestrator가 DONE 반환 → 메인 Claude가 요약을 사용자에게 제시:
```
[DEV ORCHESTRATION 완료]

TASK_ID : ...
SCOPE   : BE-only | FE-only | Fullstack
결과    : 변경 파일 N개, 테스트 M개

다음 단계 (사용자 명시 승인 필요):
  - BE: cd /Users/jaehyun/Documents/workspace/tourwave && ./scripts/task-finish.sh <TASK_ID>
  - FE: cd /Users/jaehyun/Documents/workspace/tourwave-web && ./scripts/task-finish.sh <TASK_ID>

대화 로그: ~/.claude/orchestrator-sessions/<TASK_ID>/dialog.md
```

---

## 참고

- 각 에이전트 정의:
  - `.claude/agents/orchestrator.md`
  - `.claude/agents/tdd-backend.md`
  - `.claude/agents/tdd-frontend.md`
  - `.claude/agents/security-reviewer.md`
  - `.claude/agents/verification.md`
- 기존 하네스와의 관계: `/dev`는 `/harness-task`의 풀스택 확장. 기존 `/harness-task`는 단일 repo 시나리오에 계속 사용 가능. `/dev`는 BE↔FE 협업이 필요한 작업용.
- 워크트리 및 task-finish.sh 커밋/PR 절차는 기존 하네스 설정(`harness.config.sh`)을 그대로 사용한다.
