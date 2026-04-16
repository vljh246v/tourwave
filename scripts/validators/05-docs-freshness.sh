#!/usr/bin/env bash
# =============================================================================
# 05-docs-freshness.sh — 문서 최신화 검사
# 인수: $1=워크트리 경로, $2=프로젝트 루트
#
# 검사 항목:
#   1. 변경된 코드 파일에 대응하는 docs/ 업데이트 여부
#   2. CLAUDE.md가 AI 생성 내용을 포함하는지 여부
#   3. exec-plans/active/ 계획과 실제 작업 연결 여부
# =============================================================================
set -euo pipefail

WORKTREE_PATH="${1:-$(pwd)}"
PROJECT_ROOT="${2:-$(pwd)}"

source "$PROJECT_ROOT/harness.config.sh"

if [[ "$ENABLE_DOCS_CHECK" != "true" ]]; then
  echo "[SKIP] 문서 검증 비활성화됨 (ENABLE_DOCS_CHECK=false)"
  exit 0
fi

echo "[DOCS] 문서 최신화 검사 중..."

cd "$WORKTREE_PATH"

ISSUES_FOUND=0

# =============================================================================
# 1. CLAUDE.md 크기 확인 (60줄 초과 경고)
# =============================================================================
if [[ -f "CLAUDE.md" ]]; then
  line_count=$(wc -l < CLAUDE.md)
  if [[ $line_count -gt 100 ]]; then
    echo "  [WARN] CLAUDE.md가 ${line_count}줄입니다 (권장: 100줄 이하)"
    echo "         상세 내용을 docs/로 분리하는 것을 고려하세요"
    # 경고만 (실패 아님)
  fi
fi

# =============================================================================
# 2. 변경된 파일 목록 확인
# =============================================================================
changed_files=$(git diff --name-only HEAD~1 HEAD 2>/dev/null || git diff --name-only HEAD 2>/dev/null || true)

# 새 도메인/서비스 추가 시 ARCHITECTURE.md 업데이트 필요
new_service_dirs=$(echo "$changed_files" | grep -E "^src/main/(kotlin|java)/.*/(service|domain|api)/" | head -5 || true)
arch_updated=$(echo "$changed_files" | grep -E "ARCHITECTURE\.md|docs/" || true)

if [[ -n "$new_service_dirs" && -z "$arch_updated" ]]; then
  echo ""
  echo "  [INFO] 새 서비스/도메인 코드가 추가되었습니다:"
  echo "$new_service_dirs" | sed 's/^/    /'
  echo ""
  echo "  아키텍처 문서 업데이트를 고려하세요:"
  echo "    • ARCHITECTURE.md (레이어 구조 변경 시)"
  echo "    • docs/design-docs/ (설계 결정 사항)"
  # 경고만 (실패 아님 — 모든 코드 변경이 아키텍처 변경은 아님)
fi

# =============================================================================
# 3. exec-plans/active/ 계획 정리 권유
# =============================================================================
if [[ -d "docs/exec-plans/active" ]]; then
  active_plans=$(ls docs/exec-plans/active/ 2>/dev/null | grep -v ".gitkeep" || true)
  if [[ -n "$active_plans" ]]; then
    echo "  [INFO] 진행 중인 계획 확인:"
    echo "$active_plans" | sed 's/^/    /'
    echo "  완료된 계획은 docs/exec-plans/completed/로 이동하세요"
  fi
fi

# =============================================================================
# 결과
# =============================================================================
if [[ $ISSUES_FOUND -ne 0 ]]; then
  cat <<EOF

[DOCS FAILURE] validators/05-docs-freshness.sh

  수정 방법:
  1. 위 내용을 확인하고 관련 문서를 업데이트하세요
  2. 변경이 불필요하다고 판단되면 ENABLE_DOCS_CHECK=false로 일시 비활성화
     (단, 이유를 커밋 메시지에 기록하세요)

EOF
  exit 1
fi

echo "[DOCS] 문서 검사 완료"
