#!/usr/bin/env bash
# =============================================================================
# post-tool-use.sh — 도구 실행 후 처리
# Claude Code가 Edit/Write 실행 후에 자동 호출
#
# 역할:
#   1. 세션 로그에 결과 기록
#   2. 파일 수정 후 빠른 린트 (선택적)
# =============================================================================

INPUT=$(cat)
TOOL_NAME=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_name',''))" 2>/dev/null || echo "")
TOOL_INPUT=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(json.dumps(d.get('tool_input',{})))" 2>/dev/null || echo "{}")

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

source "$PROJECT_ROOT/harness.config.sh" 2>/dev/null || true

# 현재 워크트리 이름 추출
CURRENT_PATH="$(pwd)"
TASK_NAME=""
if [[ "$CURRENT_PATH" == *"/worktrees/"* ]]; then
  TASK_NAME="$(echo "$CURRENT_PATH" | sed 's|.*/worktrees/||' | cut -d'/' -f1)"
fi

# =============================================================================
# 1. 세션 로그 기록
# =============================================================================
if [[ -n "$TASK_NAME" ]]; then
  SESSION_LOG="${PROJECT_ROOT}/${LOG_DIR:-logs}/sessions/${TASK_NAME}/session.jsonl"
  if [[ -f "$SESSION_LOG" ]]; then
    if [[ "$TOOL_NAME" == "Edit" || "$TOOL_NAME" == "Write" ]]; then
      file_path=$(echo "$TOOL_INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('file_path',''))" 2>/dev/null || echo "")
      echo "{\"ts\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"event\":\"tool_use\",\"tool\":\"${TOOL_NAME}\",\"file\":$(echo "$file_path" | python3 -c 'import sys,json; print(json.dumps(sys.stdin.read().strip()))' 2>/dev/null || echo '""')}" >> "$SESSION_LOG"
    fi
  fi
fi

# =============================================================================
# 2. 빠른 린트 (Edit/Write 후, 선택적)
# QUICK_LINT=true 설정 시 활성화
# =============================================================================
if [[ "${QUICK_LINT:-false}" == "true" && ("$TOOL_NAME" == "Edit" || "$TOOL_NAME" == "Write") ]]; then
  file_path=$(echo "$TOOL_INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('file_path',''))" 2>/dev/null || echo "")

  if [[ -n "$file_path" && -f "$file_path" ]]; then
    ext="${file_path##*.}"

    case "$ext" in
      kt|kts)
        # Kotlin: ktlint 단일 파일
        if command -v ktlint &>/dev/null; then
          ktlint "$file_path" 2>/dev/null || true
        fi
        ;;
      ts|tsx|js|jsx)
        # TypeScript: eslint 단일 파일
        if command -v eslint &>/dev/null; then
          eslint "$file_path" --quiet 2>/dev/null || true
        fi
        ;;
      py)
        # Python: ruff 단일 파일
        if command -v ruff &>/dev/null; then
          ruff check "$file_path" 2>/dev/null || true
        fi
        ;;
    esac
  fi
fi

exit 0
