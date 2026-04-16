#!/usr/bin/env bash
# =============================================================================
# 03-lint.sh — 린트 검사 (코드 스타일 + 아키텍처 규칙)
# 인수: $1=워크트리 경로, $2=프로젝트 루트
# =============================================================================
set -euo pipefail

WORKTREE_PATH="${1:-$(pwd)}"
PROJECT_ROOT="${2:-$(pwd)}"

source "$PROJECT_ROOT/harness.config.sh"

if [[ "$ENABLE_LINT_CHECK" != "true" ]]; then
  echo "[SKIP] 린트 검증 비활성화됨 (ENABLE_LINT_CHECK=false)"
  exit 0
fi

echo "[LINT] 린트 실행 중..."
echo "  커맨드: $LINT_CMD"
echo "  경로:   $WORKTREE_PATH"

cd "$WORKTREE_PATH"

set +e
output=$(eval "$LINT_CMD" 2>&1)
exit_code=$?
set -e

if [[ $exit_code -ne 0 ]]; then
  cat <<EOF

[LINT FAILURE] validators/03-lint.sh
  커맨드: $LINT_CMD
  종료 코드: $exit_code

  린트 오류:
$(echo "$output" | head -40 | sed 's/^/  /')

  수정 방법:
  1. 위 파일과 라인 번호를 확인하세요
  2. 자동 수정 가능한 경우:
     - Kotlin: ./gradlew ktlintFormat
     - TypeScript: npm run lint -- --fix
     - Python: ruff check . --fix
  3. 아키텍처 위반인 경우: ARCHITECTURE.md#layer-rules 참고

  린트 규칙 위치:
  - Kotlin: .editorconfig, ktlint 설정
  - TypeScript: .eslintrc, tsconfig.json
  - Python: pyproject.toml [tool.ruff]

EOF
  exit 1
fi

echo "[LINT] 린트 통과"
