# /harness-task — 하네스 작업 시작 → 구현 → 검증 → 병합

자연어로 작업 내용을 입력받아, 계획 수립부터 구현 → 검증 → 완료까지 전체 플로우를 자동으로 수행한다.
`harness.config.sh`의 Git Flow 설정에 따라 단계(개발/QA/핫픽스)별로 다른 브랜치 전략을 사용한다.

---

## Step 0. 기존 작업 이어하기 판단

명령을 받으면 먼저 **이어하기인지 새 작업인지** 판단한다.

### 판단 기준

1. 입력에 기존 task-id가 포함되어 있는지 확인한다
   - 예: `/harness-task PROJ-102 이어서 해줘`, `/harness-task PROJ-102 계속`
2. 해당 task-id의 워크트리가 `.worktrees/<task-id>`에 존재하는지 확인한다
3. 해당 task-id의 exec-plan이 `docs/exec-plans/active/<task-id>.md` 또는 `docs/exec-plans/active/*<task-id>*`에 존재하는지 확인한다

### 이어하기인 경우 (워크트리 + exec-plan 존재)

Step 1~5를 전부 건너뛰고 아래를 수행한다:

1. exec-plan을 읽어서 현재 상태를 파악한다
2. `agent-failures.md`에서 이 task-id 관련 기록이 있는지 확인한다
   - 있으면: "이전에 이런 이유로 실패했습니다"를 사용자에게 보여준다
3. 사용자에게 상황을 정리해서 보여준다:
   ```
   [HARNESS-TASK] 기존 작업 재개

     Task ID  : PROJ-102
     워크트리 : .worktrees/PROJ-102 (존재)
     exec-plan: docs/exec-plans/active/PROJ-102.md
     이전 실패: F-2026-04-15-1 (02-test 실패, 정책 변경 시 테스트 미수정)

     어떻게 진행할까요?
     1. 이전 실패 원인을 해결하고 이어서 진행
     2. 처음부터 다시 (워크트리 삭제 후 새로 시작)
   ```
4. 사용자가 1을 선택하면 → **Step 6 (구현)**으로 바로 진입. 이전 실패 기록을 참고하면서 수정
5. 사용자가 2를 선택하면 → 워크트리/브랜치 삭제 후 Step 1부터 새로 시작

### 새 작업인 경우 (워크트리 없음)

Step 1부터 정상 진행한다.

---

## Step 1. 입력 분석

사용자의 자연어 입력에서 아래를 추출한다.

### Task ID 결정

1. 입력에 Jira 티켓 ID가 포함되어 있으면 그대로 사용
   - 예: "PROJ-203 로그인 버그 수정" → task-id = `PROJ-203`

2. 입력에 티켓 ID가 없으면 사용자에게 물어본다
   - "Jira 티켓 번호가 있으면 알려주세요 (예: PROJ-203). 없으면 Enter"
   - 티켓 번호를 알려주면 → 그대로 task-id로 사용
   - Enter(없음)를 선택하면 → 타임스탬프 기반 자동 생성
     - `date +%Y%m%d-%H%M%S`를 Bash로 실행
     - task-id = `TASK-{YYYYMMDD-HHMMSS}` (예: `TASK-20260415-143022`)

### Task Type 결정

| Type | 기준 |
|------|------|
| `feat` | 새 기능 추가, API 신규, 화면 추가 |
| `fix` | 버그 수정, 오류 해결, 예외 처리 |
| `refactor` | 리팩터링, 구조 개선, 성능 최적화 |
| `chore` | 설정 변경, 빌드, 의존성 업데이트 |
| `docs` | 문서 작성, README 수정, 주석 추가 |

### 한 줄 요약 작성

작업 내용을 50자 이내 한국어로 요약한다.

---

## Step 2. 작업 단계 확인 (Git Flow)

`harness.config.sh`의 `MERGE_STRATEGY`가 `"pr"`이면 사용자에게 작업 단계를 물어본다:

```
어떤 단계에서 작업하시나요?
  1. 개발    (develop 기반 → upstream/develop에 PR)
  2. QA/릴리즈 (release/x.x.x 기반 → upstream/release에 PR)
  3. 핫픽스   (main 기반 → upstream/main에 PR → develop, release에도 머지)
```

단계에 따라 base 브랜치를 결정한다:
- **개발**: `harness.config.sh`의 `PHASE_DEVELOP_BASE` 사용 (기본: `develop`)
- **QA/릴리즈**: `PHASE_RELEASE_BASE`가 비어있으면 사용자에게 물어본다
  - "릴리즈 브랜치명을 알려주세요 (예: release/1.2.0)"
- **핫픽스**: `PHASE_HOTFIX_BASE` 사용 (기본: `main`)

`MERGE_STRATEGY`가 `"direct"`이면 이 단계를 건너뛰고 `BASE_BRANCH`를 사용한다.

---

## Step 3. 분석 결과 출력 + 확인

사용자에게 결과를 보여주고 확인을 받는다:

```
[HARNESS-TASK] 분석 결과

  Task ID  : PROJ-203
  Type     : fix
  요약     : 로그인 페이지 토큰 만료 시 null 오류 수정
  단계     : QA/릴리즈
  Base     : release/1.2.0
  브랜치   : feature/PROJ-203
  병합 대상 : upstream/release/1.2.0 (PR)

계속 진행할까요?
```

---

## Step 4. 실행 계획 문서 생성

`docs/exec-plans/active/<task-id>.md` 를 생성한다. (task-start.sh가 스켈레톤을 이미 만들었으면 그 파일을 채운다)

```markdown
---
task_id: <task-id>
type: <type>
phase: develop | release | hotfix
base_branch: <결정된 base 브랜치>
status: in-progress
created: <현재 시각 ISO8601>
owner: (작성자)
---

# <type>: <요약> (<task-id>)

## 배경 및 목표

[사용자 입력을 기반으로 작성]

## 구현 단계

- [ ] 1. 영향 범위 파악
- [ ] 2. [작업 내용에 맞는 구현 단계들]
- [ ] 3. 테스트 작성/수정
- [ ] 4. verify-task.sh로 검증
- [ ] 5. task-finish.sh로 완료

## 완료 기준

- [ ] 검증기 5개 전부 통과
- [ ] 기존 테스트 통과

## 참고

- ARCHITECTURE.md
- docs/golden-principles.md
- logs/trends/failure-patterns.md
```

---

## Step 5. 브랜치 동기화 + 워크트리 생성

`harness.config.sh`의 `GIT_REMOTE` (기본: `upstream`)를 사용하여 base 브랜치를 동기화한다:

```bash
# 1. upstream에서 최신 코드 가져오기
git fetch <GIT_REMOTE> <base-branch>

# 2. 로컬 base 브랜치 동기화
git checkout <base-branch>
git merge --ff-only <GIT_REMOTE>/<base-branch>

# 3. 워크트리 생성
./scripts/task-start.sh <task-id>
```

`task-start.sh`가 `harness.config.sh`의 `BASE_BRANCH`를 읽으므로,
실행 전에 `BASE_BRANCH`를 현재 단계에 맞게 export 해야 한다:

```bash
export BASE_BRANCH="<결정된 base 브랜치>"
./scripts/task-start.sh <task-id>
```

failure-patterns.md 미리보기를 사용자에게 그대로 보여준다.

---

## Step 6. 구현

**확인을 받은 후, 바로 구현에 들어간다. 여기서 멈추지 않는다.**

### 6-1. 구현 전 체크 (Feedforward)

코드를 작성하기 전에 아래 문서를 반드시 읽는다:

1. **`docs/golden-principles.md`** 전체를 읽는다
   - GP 규칙에 위반되는 구현을 하지 않도록 미리 확인
   - 예: GP-003("테스트 없이 비즈니스 로직 추가 금지")이 있으면 테스트를 반드시 같이 작성

2. **`docs/escalation-policy.md`**에서 이번 작업이 고위험 카테고리에 해당하는지 확인한다
   - **해당하는 경우**: 즉시 사용자에게 알리고 승인을 받는다
     ```
     [ESCALATION] 이 작업은 에스컬레이션 정책에 해당합니다.

       카테고리: 데이터베이스 스키마 변경
       정책: docs/escalation-policy.md#1

       exec-plan에 다음을 추가해주세요:
       - Impact: 이 변경의 영향 범위
       - Rollback: 문제 시 되돌리는 방법
       - Verification: 정상 동작 확인 방법

       승인 후 진행하겠습니다. 계속할까요?
     ```
   - **해당하지 않는 경우**: 그대로 구현 진행

3. **`logs/trends/failure-patterns.md`**에서 이번 작업과 관련된 과거 실패가 있는지 확인
   - 있으면 해당 패턴을 피하면서 구현

### 6-2. 구현

1. 워크트리 디렉토리(`.worktrees/<task-id>`)로 이동하여 작업한다
2. `ARCHITECTURE.md`의 레이어 규칙을 준수한다
3. 계획 문서의 구현 단계를 하나씩 실행한다:
   - 관련 파일 읽기 → 코드 수정/추가 → 테스트 작성/수정
4. 모든 코드 변경은 워크트리 내부 파일만 대상으로 한다
5. 구현 완료 후 워크트리에서 `git add` + `git commit` 한다
   - 커밋 메시지: `<type>: <task-id> <요약>`
   - 예: `feat: PROJ-101 Todo 완료 상태 필터 추가`

---

## Step 7. 검증 + 완료

### 검증 실패 시 대응 (공통)

`verify-task.sh`가 실패하면 아래 순서로 판단한다:

1. **exec-plan 확인**: `docs/exec-plans/active/<task-id>.md`를 읽는다
2. **실패 원인 분류**:
   - **빌드 실패 (01-build)**: 컴파일 오류 → 코드 수정
   - **테스트 실패 (02-test)**: 아래 두 경우를 구분
     - **A. 기존 동작을 깨뜨린 경우**: exec-plan의 목표와 관련 없는 테스트가 깨졌으면 → 내 코드가 잘못된 것. 코드를 수정한다
     - **B. 의도적 정책 변경인 경우**: exec-plan에 명시된 변경 때문에 기존 테스트가 깨졌으면 → 테스트가 옛날 정책을 기대하고 있는 것. 테스트도 함께 수정한다
   - **린트 실패 (03-lint)**: `ktlintFormat` 등 자동 수정 시도
   - **보안 실패 (04-security)**: .env나 시크릿 제거
   - **문서 실패 (05-docs)**: 문서 업데이트
3. **수정 → 재커밋 → verify-task.sh 재실행**
4. **최대 3회 시도**. 3회 실패하면:

   **a) `docs/agent-failures.md`에 자동 기록한다:**
   - 현재 날짜로 ID 생성 (`date +%Y-%m-%d`를 Bash로 실행)
   - 기존 F- 항목 수를 세어 같은 날 번호 결정
   - 아래 형식으로 테이블에 행을 추가:
   ```
   | F-YYYY-MM-DD-N | <실패 증상 요약> | <근본 원인 추정> | <시도한 대응> | <도메인> |
   ```
   - 예시:
   ```
   | F-2026-04-15-1 | 02-test: createTodo 빈 제목 테스트 실패 | 정책 변경인데 테스트 미수정 | exec-plan 확인 후 테스트 수정 시도했으나 실패 | 테스트 |
   ```

   **b) 사용자에게 보고한다:**
   ```
   [HARNESS-TASK] 자동 수정 3회 실패. 사람 확인이 필요합니다.
     실패 검증기: 02-test
     판단 필요: 코드가 잘못된 건지, 테스트를 바꿔야 하는 건지
     exec-plan: docs/exec-plans/active/<task-id>.md
     워크트리: .worktrees/<task-id>
     기록됨: docs/agent-failures.md (F-2026-04-15-1)
   ```

### MERGE_STRATEGY = "pr" (실제 프로젝트)

1. `verify-task.sh <task-id>` 실행하여 검증기 5개 통과 확인
2. 실패 시 위 "검증 실패 시 대응" 절차를 따른다
3. 전부 통과하면:
   ```bash
   # 워크트리에서 origin으로 push
   cd .worktrees/<task-id>
   git push origin feature/<task-id>
   ```
4. 사용자에게 PR 생성 안내:
   ```
   [HARNESS-TASK] 검증 통과. PR을 생성해주세요:

     origin/feature/<task-id> → <GIT_REMOTE>/<base-branch>

   PR 제목: <type>: <task-id> <요약>
   ```
5. 워크트리는 PR 머지 후 사용자가 직접 정리하거나 `task-cleanup.sh`가 처리

### MERGE_STRATEGY = "direct" (로컬 데모)

1. `task-finish.sh <task-id>` 실행 (검증 + 로컬 병합 + 워크트리 정리)
2. 실패 시 위 "검증 실패 시 대응" 절차를 따른다

### 핫픽스인 경우 추가 안내

핫픽스 작업이면 PR 머지 후 추가 작업이 필요하다. 사용자에게 안내한다:

```
[HARNESS-TASK] 핫픽스 추가 작업 안내

  main에 머지 후 다음 브랜치에도 변경사항을 반영해야 합니다:
  1. release/<현재 릴리즈 버전> (QA 진행 중인 경우)
  2. develop

  방법: cherry-pick 또는 merge PR 생성
```

---

## Step 8. 완료 보고

```
[HARNESS-TASK] 완료

  Task ID   : <task-id>
  단계      : 개발 / QA / 핫픽스
  Base      : <base-branch>
  상태      : ✅ push 완료 (PR 생성 필요) / ✅ 병합 완료 / ❌ 검증 실패
  변경 파일  : [변경된 파일 목록]
  검증 결과  : PASS 5 / FAIL 0
```

성공 시 계획 문서를 `docs/exec-plans/completed/`로 이동한다.

---

## 주의사항

- **Step 6에서 멈추지 않는다.** 계획 + 워크트리 생성 후 바로 구현에 들어간다.
- `task-start.sh` 실행 전에 반드시 계획 문서를 먼저 생성한다
- `MERGE_STRATEGY="pr"`일 때 `task-finish.sh`로 직접 병합하지 않는다 — 대신 push + PR 안내
- 코드 수정은 반드시 `.worktrees/<task-id>/` 내부 파일만 대상으로 한다
- 실패 시 자동 수정을 시도하되, 원인을 모르겠으면 사용자에게 물어본다
- 핫픽스 후 develop/release에 변경 반영하는 것은 사용자 몫 — 안내만 한다
