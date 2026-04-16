#!/usr/bin/env bash
# =============================================================================
# task-cleanup.sh — 스테일 워크트리 정리 (GC 에이전트가 주기적 실행)
# 사용법: ./scripts/task-cleanup.sh [--dry-run]
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

source "$PROJECT_ROOT/harness.config.sh"

DRY_RUN=false
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=true

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " [task-cleanup] 워크트리 GC$([ "$DRY_RUN" = true ] && echo ' (DRY RUN)' || echo '')"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

WORKTREE_ROOT_PATH="${PROJECT_ROOT}/${WORKTREE_ROOT}"

if [[ ! -d "$WORKTREE_ROOT_PATH" ]]; then
  echo " 워크트리 디렉토리가 없습니다: $WORKTREE_ROOT_PATH"
  echo " 정리할 항목이 없습니다."
  exit 0
fi

# git worktree list로 현재 워크트리 상태 확인
echo " 현재 워크트리 목록:"
git -C "$PROJECT_ROOT" worktree list
echo ""

CLEANED=0
KEPT=0

for worktree_path in "$WORKTREE_ROOT_PATH"/*/; do
  [[ -d "$worktree_path" ]] || continue

  task_name="$(basename "$worktree_path")"
  branch_name="${BRANCH_PREFIX:-feature}/${task_name}"

  # 브랜치가 이미 병합/삭제된 경우
  if ! git -C "$PROJECT_ROOT" rev-parse --verify "$branch_name" &>/dev/null; then
    echo " [STALE] $task_name — 브랜치 없음, 제거 대상"
    if [[ "$DRY_RUN" == "false" ]]; then
      git -C "$PROJECT_ROOT" worktree remove --force "$worktree_path" 2>/dev/null || rm -rf "$worktree_path"
      echo "         → 제거 완료"
    fi
    (( CLEANED++ )) || true
    continue
  fi

  # 7일 이내에 수정된 소스 파일이 있는지 확인 (괄호로 -o 우선순위 명시)
  recent_file=$(find "$worktree_path" \( -name "*.kt" -o -name "*.ts" -o -name "*.py" -o -name "*.java" \) -mtime -7 2>/dev/null | head -1)
  if [[ -z "$recent_file" ]]; then
    echo " [IDLE]  $task_name — 7일 이상 비활성 (경고: 정리 고려)"
    (( KEPT++ )) || true
  else
    echo " [ACTIVE] $task_name — 작업 중 (보존)"
    (( KEPT++ )) || true
  fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " 결과: 제거 $CLEANED / 보존 $KEPT"
[ "$DRY_RUN" = true ] && echo " (DRY RUN — 실제 제거 없음)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
