#!/usr/bin/env bash
# =============================================================================
# pre-tool-use.sh — 도구 실행 전 검증
# Claude Code가 Bash/Edit/Write 실행 전에 자동 호출
#
# 입력: STDIN으로 JSON 이벤트 수신
# 출력: exit 0 = 허용 / exit 1 = 차단 / exit 2 = 경고(계속 진행)
# =============================================================================

# 이벤트 파싱
INPUT=$(cat)
TOOL_NAME=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_name',''))" 2>/dev/null || echo "")
TOOL_INPUT=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(json.dumps(d.get('tool_input',{})))" 2>/dev/null || echo "{}")

# =============================================================================
# 1. 세션 로그에 이벤트 기록
# =============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

source "$PROJECT_ROOT/harness.config.sh" 2>/dev/null || true

# 현재 워크트리 이름 추출 (경로 기반)
CURRENT_PATH="$(pwd)"
TASK_NAME=""
if [[ "$CURRENT_PATH" == *"/worktrees/"* ]]; then
  TASK_NAME="$(echo "$CURRENT_PATH" | sed 's|.*/worktrees/||' | cut -d'/' -f1)"
fi

if [[ -n "$TASK_NAME" ]]; then
  SESSION_LOG="${PROJECT_ROOT}/${LOG_DIR:-logs}/sessions/${TASK_NAME}/session.jsonl"
  if [[ -f "$SESSION_LOG" ]]; then
    # 도구 사용 이벤트 기록
    cmd_preview=$(echo "$TOOL_INPUT" | python3 -c "
import sys, json
d = json.load(sys.stdin)
cmd = d.get('command', d.get('file_path', d.get('path', '')))
print(str(cmd)[:80])
" 2>/dev/null || echo "")

    echo "{\"ts\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"event\":\"pre_tool\",\"tool\":\"${TOOL_NAME}\",\"preview\":$(echo "$cmd_preview" | python3 -c 'import sys,json; print(json.dumps(sys.stdin.read().strip()))' 2>/dev/null || echo '""')}" >> "$SESSION_LOG"
  fi
fi

# =============================================================================
# 2. 워크트리 외부 수정 경고
# =============================================================================
if [[ "$TOOL_NAME" == "Edit" || "$TOOL_NAME" == "Write" ]]; then
  file_path=$(echo "$TOOL_INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('file_path',''))" 2>/dev/null || echo "")

  # main 프로젝트 루트의 핵심 파일 직접 수정 시도 감지
  if [[ -n "$file_path" ]]; then
    abs_path=$(realpath "$file_path" 2>/dev/null || echo "$file_path")
    project_root_real=$(realpath "$PROJECT_ROOT" 2>/dev/null || echo "$PROJECT_ROOT")

    # 워크트리 외부에서 src/ 파일 수정 시도
    if [[ "$CURRENT_PATH" != *"/worktrees/"* ]] && [[ "$abs_path" == "$project_root_real/src/"* ]]; then
      cat >&2 <<EOF

[HARNESS WARNING] pre-tool-use.sh
  소스 코드를 워크트리 외부에서 직접 수정하려 합니다.

  현재 경로: $CURRENT_PATH
  수정 대상: $file_path

  권장 방법:
  1. ./scripts/task-start.sh <task-name>으로 워크트리 생성
  2. 워크트리 내에서 작업
  3. ./scripts/task-finish.sh <task-name>으로 검증 후 병합

  계속 진행하려면 워크트리를 사용하거나 이 경고를 무시하세요.

EOF
      # exit 2 = 경고이지만 차단하지 않음 (사용자가 결정)
      exit 2
    fi
  fi
fi

# =============================================================================
# 3. 위험한 Bash 명령 차단
# =============================================================================
if [[ "$TOOL_NAME" == "Bash" ]]; then
  command=$(echo "$TOOL_INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('command',''))" 2>/dev/null || echo "")

  # 강제 푸시 차단
  if echo "$command" | grep -qE "git push.*--force|git push.*-f "; then
    cat >&2 <<EOF

[HARNESS BLOCKED] pre-tool-use.sh
  force push가 차단되었습니다.

  커맨드: $command

  이유: force push는 팀 히스토리를 파괴할 수 있습니다.
  대안: 리베이스나 머지 커밋을 사용하세요.

EOF
    exit 1
  fi

  # 재귀 삭제 차단
  if echo "$command" | grep -qE "rm -rf /|rm -rf \$HOME"; then
    cat >&2 <<EOF

[HARNESS BLOCKED] pre-tool-use.sh
  위험한 삭제 명령이 차단되었습니다.

  커맨드: $command

EOF
    exit 1
  fi
fi

exit 0
