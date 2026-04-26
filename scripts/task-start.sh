#!/usr/bin/env bash
# =============================================================================
# task-start.sh — 새 작업을 위한 격리된 워크트리 생성
# 사용법: ./scripts/task-start.sh <task-name>
# 예시:   ./scripts/task-start.sh feat-login
#         ./scripts/task-start.sh fix-auth-bug
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

source "$PROJECT_ROOT/harness.config.sh"

TASK_NAME="${1:-}"
if [[ -z "$TASK_NAME" ]]; then
  echo "[ERROR] task-start.sh: task-name을 입력하세요"
  echo "  사용법: ./scripts/task-start.sh <task-name>"
  exit 1
fi

BRANCH_NAME="${BRANCH_PREFIX:-feature}/${TASK_NAME}"
WORKTREE_PATH="${PROJECT_ROOT}/${WORKTREE_ROOT}/${TASK_NAME}"
SESSION_DIR="${PROJECT_ROOT}/${LOG_DIR}/sessions/${TASK_NAME}"
SESSION_LOG="${SESSION_DIR}/session.jsonl"

# =============================================================================
# 1. 이미 존재하는 워크트리 확인
# =============================================================================
if [[ -d "$WORKTREE_PATH" ]]; then
  echo "[WARN] 워크트리가 이미 존재합니다: $WORKTREE_PATH"
  echo "  기존 워크트리를 계속 사용합니다."
  echo ""
  echo "[INFO] 워크트리 경로: $WORKTREE_PATH"
  echo "[INFO] 세션 로그: $SESSION_LOG"
  exit 0
fi

# =============================================================================
# 1.5. Task card status: backlog → in-progress (develop 에서, 워크트리 생성 전)
#      → Obsidian 이 즉시 in-progress 를 봄. 워크트리는 이 커밋 이후 HEAD 에서 분기.
# =============================================================================
current_branch=$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD)
if [[ "$current_branch" != "$BASE_BRANCH" ]]; then
  echo "[ERROR] task-start.sh 는 $BASE_BRANCH 에서 실행되어야 합니다. 현재: $current_branch" >&2
  echo "  'git checkout $BASE_BRANCH' 후 재시도하세요." >&2
  exit 1
fi

python3 "$SCRIPT_DIR/task-status-sync.py" set \
  --task-id "$TASK_NAME" \
  --status in-progress \
  --repo-root "$PROJECT_ROOT" \
  --commit \
  --commit-message "chore: $TASK_NAME 작업 시작 (status → in-progress)" \
  2>&1 || echo "[WARN] task card status 패치 실패 (파이프라인은 계속)" >&2

# =============================================================================
# 2. 워크트리 생성
# =============================================================================
echo "[INFO] 워크트리 생성 중..."
echo "  태스크: $TASK_NAME"
echo "  브랜치: $BRANCH_NAME"
echo "  경로:   $WORKTREE_PATH"
echo ""

mkdir -p "$(dirname "$WORKTREE_PATH")"
git -C "$PROJECT_ROOT" worktree add "$WORKTREE_PATH" -b "$BRANCH_NAME" "$BASE_BRANCH"

# =============================================================================
# 3. 세션 로그 초기화
# =============================================================================
mkdir -p "$SESSION_DIR"

log_event() {
  echo "{\"ts\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"event\":\"$1\"$([ $# -gt 1 ] && echo ",$2" || echo "")}" >> "$SESSION_LOG"
}

log_event "task_start" \
  "\"task\":\"${TASK_NAME}\",\"branch\":\"${BRANCH_NAME}\",\"worktree\":\"${WORKTREE_PATH}\""

# =============================================================================
# 4. exec-plan 스켈레톤 자동 생성
# =============================================================================
EXEC_PLAN_DIR="${WORKTREE_PATH}/docs/exec-plans/active"
EXEC_PLAN_FILE="${EXEC_PLAN_DIR}/${TASK_NAME}.md"

mkdir -p "$EXEC_PLAN_DIR"

if [[ ! -f "$EXEC_PLAN_FILE" ]]; then
  cat > "$EXEC_PLAN_FILE" << PLAN
---
task_id: ${TASK_NAME}
type: feat
created: $(date -u +%Y-%m-%dT%H:%M:%SZ)
owner: (작성자)
---

# ${TASK_NAME}

## 배경 및 목표

[작업 목표를 작성하세요]

## 구현 단계

- [ ] 1. 영향 범위 파악 (관련 파일 확인)
- [ ] 2. 구현
- [ ] 3. 테스트 작성/수정
- [ ] 4. verify-task.sh로 검증
- [ ] 5. task-finish.sh로 병합

## 영향 범위

[레이어/파일 작성]

## 완료 기준

- [ ] 검증기 5개 전부 통과
- [ ] 기존 테스트 통과

## 참고

- ARCHITECTURE.md
- docs/golden-principles.md
- logs/trends/failure-patterns.md
PLAN

  log_event "exec_plan_created" "\"path\":\"${EXEC_PLAN_FILE}\""
  echo "[INFO] exec-plan 생성됨: $EXEC_PLAN_FILE"
else
  echo "[INFO] exec-plan 이미 존재: $EXEC_PLAN_FILE"
fi

# =============================================================================
# 5. failure-patterns.md 읽기 안내
# =============================================================================
FAILURE_PATTERNS="${PROJECT_ROOT}/${LOG_DIR}/trends/failure-patterns.md"
if [[ -f "$FAILURE_PATTERNS" ]]; then
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo " [FEEDFORWARD] 최근 실패 패턴을 확인하세요:"
  echo " $FAILURE_PATTERNS"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  # 핵심 내용 미리보기 (첫 20줄)
  head -20 "$FAILURE_PATTERNS"
  echo "  ..."
  echo ""
fi

# =============================================================================
# 6. 완료 메시지
# =============================================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " [SUCCESS] 워크트리 생성 완료"
echo ""
echo " 다음 단계:"
echo "   cd $WORKTREE_PATH   ← 이 디렉토리에서 작업하세요"
echo ""
echo " 작업 완료 후:"
echo "   cd $PROJECT_ROOT"
echo "   ./scripts/task-finish.sh $TASK_NAME"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
