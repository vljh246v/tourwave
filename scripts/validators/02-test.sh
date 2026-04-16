#!/usr/bin/env bash
# =============================================================================
# 02-test.sh — 테스트 실행 및 통과 여부 검증
# 인수: $1=워크트리 경로, $2=프로젝트 루트
# =============================================================================
set -euo pipefail

WORKTREE_PATH="${1:-$(pwd)}"
PROJECT_ROOT="${2:-$(pwd)}"

source "$PROJECT_ROOT/harness.config.sh"

if [[ "$ENABLE_TEST_CHECK" != "true" ]]; then
  echo "[SKIP] 테스트 검증 비활성화됨 (ENABLE_TEST_CHECK=false)"
  exit 0
fi

echo "[TEST] 테스트 실행 중..."
echo "  커맨드: $TEST_CMD"
echo "  경로:   $WORKTREE_PATH"

cd "$WORKTREE_PATH"

set +e
output=$(eval "$TEST_CMD" 2>&1)
exit_code=$?
set -e

if [[ $exit_code -ne 0 ]]; then
  # 실패한 테스트 케이스 추출 시도
  failed_tests=$(echo "$output" | grep -E "(FAIL|FAILED|ERROR|✗|×)" | head -10 || true)

  cat <<EOF

[TEST FAILURE] validators/02-test.sh
  커맨드: $TEST_CMD
  종료 코드: $exit_code

  실패한 테스트:
$(echo "${failed_tests:-'(위 출력을 확인하세요)'}" | sed 's/^/  /')

  전체 출력 (마지막 30줄):
$(echo "$output" | tail -30 | sed 's/^/  /')

  수정 방법:
  1. 실패한 테스트 케이스를 확인하세요
  2. 테스트가 기대하는 동작과 실제 구현을 비교하세요
  3. 새 기능 추가 시: 해당 기능 테스트도 함께 작성하세요
  4. 리팩터링 시: 테스트 로직은 변경하지 말고 구현만 수정하세요

  참고: logs/trends/failure-patterns.md (반복 실패 패턴)

EOF
  exit 1
fi

echo "[TEST] 테스트 통과"
