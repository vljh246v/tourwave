#!/usr/bin/env bash
# =============================================================================
# 01-build.sh — 빌드 성공 여부 검증
# 인수: $1=워크트리 경로, $2=프로젝트 루트
# =============================================================================
set -euo pipefail

WORKTREE_PATH="${1:-$(pwd)}"
PROJECT_ROOT="${2:-$(pwd)}"

source "$PROJECT_ROOT/harness.config.sh"

if [[ "$ENABLE_BUILD_CHECK" != "true" ]]; then
  echo "[SKIP] 빌드 검증 비활성화됨 (ENABLE_BUILD_CHECK=false)"
  exit 0
fi

echo "[BUILD] 빌드 실행 중..."
echo "  커맨드: $BUILD_CMD"
echo "  경로:   $WORKTREE_PATH"

cd "$WORKTREE_PATH"

set +e
output=$(eval "$BUILD_CMD" 2>&1)
exit_code=$?
set -e

if [[ $exit_code -ne 0 ]]; then
  cat <<EOF

[BUILD FAILURE] validators/01-build.sh
  커맨드: $BUILD_CMD
  종료 코드: $exit_code

  오류 출력:
$(echo "$output" | head -30 | sed 's/^/  /')

  수정 방법:
  1. 위 오류 메시지에서 파일명과 라인 번호를 확인하세요
  2. 컴파일 오류라면 해당 파일을 수정하세요
  3. 의존성 오류라면 build 파일(build.gradle.kts, package.json 등)을 확인하세요
  참고: ARCHITECTURE.md, docs/design-docs/core-beliefs.md

EOF
  exit 1
fi

echo "[BUILD] 빌드 성공"
