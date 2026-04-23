#!/usr/bin/env bash
# =============================================================================
# verify-task.sh — 검증만 실행 (병합하지 않음)
# 사용법: ./scripts/verify-task.sh <task-name>
#
# 구현 중 반복 실행하여 현재 상태를 확인할 수 있습니다.
# task-finish.sh와 달리 병합하지 않고 결과만 보여줍니다.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

source "$PROJECT_ROOT/harness.config.sh"

TASK_NAME="${1:-}"
if [[ -z "$TASK_NAME" ]]; then
  echo "[ERROR] verify-task.sh: task-name을 입력하세요"
  echo "  사용법: ./scripts/verify-task.sh <task-name>"
  exit 1
fi

WORKTREE_PATH="${PROJECT_ROOT}/${WORKTREE_ROOT}/${TASK_NAME}"
VALIDATOR_HISTORY="${PROJECT_ROOT}/${LOG_DIR}/validators/history.jsonl"
mkdir -p "$(dirname "$VALIDATOR_HISTORY")"

if [[ ! -d "$WORKTREE_PATH" ]]; then
  echo "[ERROR] 워크트리를 찾을 수 없습니다: $WORKTREE_PATH"
  echo "  task-start.sh를 먼저 실행했는지 확인하세요."
  exit 1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " [verify-task] 태스크: $TASK_NAME (검증만 — 병합 안 함)"
echo " 워크트리: $WORKTREE_PATH"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

VALIDATORS_DIR="$SCRIPT_DIR/validators"
PASSED=0
FAILED=0
FAILED_VALIDATORS=()

log_validator() {
  local name="$1"
  local result="$2"
  local error="${3:-}"
  local entry="{\"ts\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"task\":\"${TASK_NAME}\",\"validator\":\"${name}\",\"result\":\"${result}\",\"mode\":\"verify\""
  [[ -n "$error" ]] && entry="${entry},\"error\":$(echo "$error" | python3 -c 'import sys,json; print(json.dumps(sys.stdin.read().strip()))' 2>/dev/null || echo '""')"
  entry="${entry}}"
  echo "$entry" >> "$VALIDATOR_HISTORY"
}

for validator in \
  "$VALIDATORS_DIR/01-build.sh" \
  "$VALIDATORS_DIR/02-test.sh" \
  "$VALIDATORS_DIR/03-lint.sh" \
  "$VALIDATORS_DIR/04-security.sh" \
  "$VALIDATORS_DIR/05-docs-freshness.sh" \
  "$VALIDATORS_DIR/07-status-sync-smoke.sh" \
  "$VALIDATORS_DIR/08-propagate-smoke.sh"; do

  [[ -f "$validator" ]] || continue

  validator_name="$(basename "$validator" .sh)"
  echo "  ▶ $validator_name ..."

  start_ms=$(python3 -c 'import time; print(int(time.time()*1000))' 2>/dev/null || echo "0")

  set +e
  output=$(bash "$validator" "$WORKTREE_PATH" "$PROJECT_ROOT" 2>&1)
  exit_code=$?
  set -e

  end_ms=$(python3 -c 'import time; print(int(time.time()*1000))' 2>/dev/null || echo "0")
  duration_ms=$(( end_ms - start_ms ))

  if [[ $exit_code -eq 0 ]]; then
    echo "    ✓ PASS (${duration_ms}ms)"
    log_validator "$validator_name" "pass"
    (( PASSED++ )) || true
  else
    echo ""
    echo "    ✗ FAIL (${duration_ms}ms)"
    echo ""
    echo "$output" | sed 's/^/    /'
    echo ""
    log_validator "$validator_name" "fail" "$output"
    FAILED_VALIDATORS+=("$validator_name")
    (( FAILED++ )) || true
  fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " 검증 결과: PASS $PASSED / FAIL $FAILED"

if [[ $FAILED -gt 0 ]]; then
  echo ""
  echo " 실패한 검증기:"
  for v in "${FAILED_VALIDATORS[@]}"; do
    echo "   • $v"
  done
fi

echo ""
echo " 이 명령은 검증만 실행합니다. 병합하려면:"
echo "   ./scripts/task-finish.sh $TASK_NAME"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

[[ $FAILED -gt 0 ]] && exit 1 || exit 0
