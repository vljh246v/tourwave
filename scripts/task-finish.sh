#!/usr/bin/env bash
# =============================================================================
# task-finish.sh — 검증 통과 시에만 BASE_BRANCH로 병합
# 사용법: ./scripts/task-finish.sh <task-name>
# 예시:   ./scripts/task-finish.sh feat-login
#
# 주의: 검증기 중 하나라도 실패하면 병합이 차단됩니다.
#       실패 시 워크트리는 보존되어 디버깅에 활용할 수 있습니다.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

source "$PROJECT_ROOT/harness.config.sh"

TASK_NAME="${1:-}"
if [[ -z "$TASK_NAME" ]]; then
  echo "[ERROR] task-finish.sh: task-name을 입력하세요"
  echo "  사용법: ./scripts/task-finish.sh <task-name>"
  exit 1
fi

BRANCH_NAME="${BRANCH_PREFIX:-feature}/${TASK_NAME}"
WORKTREE_PATH="${PROJECT_ROOT}/${WORKTREE_ROOT}/${TASK_NAME}"
SESSION_LOG="${PROJECT_ROOT}/${LOG_DIR}/sessions/${TASK_NAME}/session.jsonl"
VALIDATOR_HISTORY="${PROJECT_ROOT}/${LOG_DIR}/validators/history.jsonl"
mkdir -p "$(dirname "$VALIDATOR_HISTORY")"

log_event() {
  local event="$1"
  local extra="${2:-}"
  local entry="{\"ts\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"event\":\"${event}\",\"task\":\"${TASK_NAME}\""
  [[ -n "$extra" ]] && entry="${entry},${extra}"
  entry="${entry}}"
  echo "$entry" >> "$SESSION_LOG"
}

log_validator() {
  local name="$1"
  local result="$2"
  local error="${3:-}"
  local entry="{\"ts\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"task\":\"${TASK_NAME}\",\"validator\":\"${name}\",\"result\":\"${result}\""
  [[ -n "$error" ]] && entry="${entry},\"error\":$(echo "$error" | python3 -c 'import sys,json; print(json.dumps(sys.stdin.read().strip()))')"
  entry="${entry}}"
  echo "$entry" >> "$VALIDATOR_HISTORY"
}

# 워크트리 존재 확인
if [[ ! -d "$WORKTREE_PATH" ]]; then
  echo "[ERROR] 워크트리를 찾을 수 없습니다: $WORKTREE_PATH"
  echo "  task-start.sh를 먼저 실행했는지 확인하세요."
  exit 1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " [task-finish] 태스크: $TASK_NAME"
echo " 워크트리: $WORKTREE_PATH"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

VALIDATORS_DIR="$SCRIPT_DIR/validators"
PASSED=0
FAILED=0
FAILED_VALIDATORS=()

# =============================================================================
# 검증기 순서대로 실행
# =============================================================================
run_validator() {
  local validator_script="$1"
  local validator_name
  validator_name="$(basename "$validator_script" .sh)"

  echo "  ▶ $validator_name ..."

  local start_ms
  start_ms=$(python3 -c 'import time; print(int(time.time()*1000))' 2>/dev/null || echo "0")

  local output
  local exit_code=0

  set +e
  output=$(bash "$validator_script" "$WORKTREE_PATH" "$PROJECT_ROOT" 2>&1)
  exit_code=$?
  set -e

  local end_ms
  end_ms=$(python3 -c 'import time; print(int(time.time()*1000))' 2>/dev/null || echo "0")
  local duration_ms=$(( end_ms - start_ms ))

  if [[ $exit_code -eq 0 ]]; then
    echo "    ✓ PASS (${duration_ms}ms)"
    log_event "validator" "\"name\":\"${validator_name}\",\"result\":\"pass\",\"duration_ms\":${duration_ms}"
    log_validator "$validator_name" "pass"
    (( PASSED++ )) || true
  else
    echo ""
    echo "    ✗ FAIL (${duration_ms}ms)"
    echo ""
    echo "$output" | sed 's/^/    /'
    echo ""
    log_event "validator" "\"name\":\"${validator_name}\",\"result\":\"fail\",\"duration_ms\":${duration_ms}"
    log_validator "$validator_name" "fail" "$output"
    FAILED_VALIDATORS+=("$validator_name")
    (( FAILED++ )) || true
  fi
}

# 검증기 순서대로 실행
for validator in \
  "$VALIDATORS_DIR/01-build.sh" \
  "$VALIDATORS_DIR/02-test.sh" \
  "$VALIDATORS_DIR/03-lint.sh" \
  "$VALIDATORS_DIR/04-security.sh" \
  "$VALIDATORS_DIR/05-docs-freshness.sh"; do
  if [[ -f "$validator" ]]; then
    run_validator "$validator"
  fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " 검증 결과: PASS $PASSED / FAIL $FAILED"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# =============================================================================
# 실패 시 — 병합 차단
# =============================================================================
if [[ $FAILED -gt 0 ]]; then
  echo ""
  echo " [BLOCKED] 다음 검증기가 실패하여 병합이 차단되었습니다:"
  for v in "${FAILED_VALIDATORS[@]}"; do
    echo "   • $v"
  done
  echo ""
  echo " 워크트리는 보존됩니다:"
  echo "   cd $WORKTREE_PATH"
  echo ""
  echo " ┌─────────────────────────────────────────────────────┐"
  echo " │ 다음 단계: 실패 원인을 판단하세요                      │"
  echo " │                                                     │"
  echo " │ A. 내 코드가 잘못된 경우 (기존 동작을 깨뜨림)          │"
  echo " │    → 워크트리에서 코드 수정 → task-finish.sh 재실행    │"
  echo " │                                                     │"
  echo " │ B. 의도적 변경인 경우 (정책/스펙이 바뀜)               │"
  echo " │    → 테스트도 함께 수정 → task-finish.sh 재실행        │"
  echo " │                                                     │"
  echo " │ 판단이 어려우면:                                      │"
  echo " │    → docs/exec-plans/active/ 에서 계획 확인            │"
  echo " │    → docs/escalation-policy.md 해당 여부 확인          │"
  echo " └─────────────────────────────────────────────────────┘"
  echo ""
  echo " 중간 검증만 하려면:"
  echo "   ./scripts/verify-task.sh $TASK_NAME"
  echo ""

  log_event "task_finish" "\"result\":\"blocked\",\"failed_validators\":$(printf '%s\n' "${FAILED_VALIDATORS[@]}" | python3 -c 'import sys,json; print(json.dumps([l.strip() for l in sys.stdin]))')"
  exit 1
fi

# =============================================================================
# 전부 통과 — MERGE_STRATEGY에 따라 분기
# =============================================================================
REMOTE="${GIT_REMOTE:-origin}"
STRATEGY="${MERGE_STRATEGY:-direct}"

EXEC_PLAN_DIR="${PROJECT_ROOT}/docs/exec-plans"
COMPLETED_DIR="${EXEC_PLAN_DIR}/completed"
mkdir -p "$COMPLETED_DIR"

archive_exec_plan() {
  for plan in "$EXEC_PLAN_DIR/active/"*"${TASK_NAME}"*; do
    [[ -f "$plan" ]] || continue
    mv "$plan" "$COMPLETED_DIR/"
    echo " [ARCHIVE] $(basename "$plan") → completed/"
  done
}

if [[ "$STRATEGY" == "pr" ]]; then
  # ===========================================================================
  # PR 모드: exec-plan 정리를 feature 브랜치에 포함시킨 뒤 push
  # ===========================================================================
  echo ""
  echo " [ARCHIVE] exec-plan 정리 중 (feature 브랜치에 포함)..."
  archive_exec_plan

  # 워크트리(feature 브랜치)에서 정리 커밋
  cd "$WORKTREE_PATH"
  git add "$PROJECT_ROOT/logs/validators/history.jsonl" "$EXEC_PLAN_DIR/" 2>/dev/null || true
  if ! git diff --cached --quiet 2>/dev/null; then
    git commit -m "chore: $TASK_NAME 검증 로그 + exec-plan 정리" --quiet
  fi

  echo " [PUSH] feature 브랜치를 push..."

  git push "${REMOTE}" "$BRANCH_NAME" 2>&1 || {
    echo "[BLOCKED] push 실패. ${REMOTE} 리모트 설정을 확인하세요."
    exit 1
  }

  log_event "task_finish" "\"result\":\"pushed\",\"strategy\":\"pr\",\"validators_passed\":${PASSED}"

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo " [PR 생성 필요]"
  echo ""
  echo "   origin/$BRANCH_NAME → $REMOTE/$BASE_BRANCH"
  echo ""
  echo " 워크트리는 PR 머지 후 정리하세요:"
  echo "   ./scripts/task-cleanup.sh"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

else
  # ===========================================================================
  # Direct 모드: 로컬에서 직접 병합 후 정리
  # ===========================================================================
  echo ""
  echo " [SYNCING] $BASE_BRANCH 동기화 중..."

  git -C "$PROJECT_ROOT" checkout "$BASE_BRANCH"

  # 리모트가 있으면 동기화
  if git -C "$PROJECT_ROOT" remote | grep -q "^${REMOTE}$"; then
    if ! git -C "$PROJECT_ROOT" fetch "$REMOTE" "$BASE_BRANCH"; then
      echo "[BLOCKED] $REMOTE/$BASE_BRANCH fetch 실패. 네트워크/권한/remote 설정 확인 필요"
      exit 1
    fi
    if ! git -C "$PROJECT_ROOT" merge --ff-only "$REMOTE/$BASE_BRANCH"; then
      echo "[BLOCKED] 로컬 $BASE_BRANCH 가 $REMOTE/$BASE_BRANCH 와 fast-forward 불가"
      echo "          $BASE_BRANCH 충돌 해결 후 다시 실행하세요."
      exit 1
    fi
  else
    echo " [INFO] $REMOTE 리모트 없음 — 로컬 전용 모드"
  fi

  echo " [MERGING] $BRANCH_NAME → $BASE_BRANCH ..."
  git -C "$PROJECT_ROOT" merge "$BRANCH_NAME" --no-ff -m "merge: $TASK_NAME"

  echo " [CLEANUP] 워크트리 제거 중..."
  git -C "$PROJECT_ROOT" worktree remove "$WORKTREE_PATH"
  git -C "$PROJECT_ROOT" branch -d "$BRANCH_NAME" 2>/dev/null || true

  # merge 후 develop에서 exec-plan 정리 + 자동 커밋
  archive_exec_plan
  cd "$PROJECT_ROOT"
  git add logs/validators/history.jsonl docs/exec-plans/ 2>/dev/null || true
  if ! git diff --cached --quiet 2>/dev/null; then
    git commit -m "chore: $TASK_NAME 검증 로그 + exec-plan 정리" --quiet
  fi

  log_event "task_finish" "\"result\":\"success\",\"strategy\":\"direct\",\"validators_passed\":${PASSED}"

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo " [SUCCESS] $TASK_NAME 완료 및 병합됨"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
fi
