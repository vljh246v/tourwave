#!/usr/bin/env bash
# =============================================================================
# 04-security.sh — 시크릿/취약점 스캔 (언어 무관)
# 인수: $1=워크트리 경로, $2=프로젝트 루트
#
# 사용 도구 (우선순위):
#   1. gitleaks (brew install gitleaks)
#   2. truffleHog
#   3. 직접 패턴 매칭 (폴백)
# =============================================================================
set -euo pipefail

WORKTREE_PATH="${1:-$(pwd)}"
PROJECT_ROOT="${2:-$(pwd)}"

source "$PROJECT_ROOT/harness.config.sh"

if [[ "$ENABLE_SECURITY_CHECK" != "true" ]]; then
  echo "[SKIP] 보안 검증 비활성화됨 (ENABLE_SECURITY_CHECK=false)"
  exit 0
fi

echo "[SECURITY] 보안 스캔 중..."
echo "  경로: $WORKTREE_PATH"

cd "$WORKTREE_PATH"

ISSUES_FOUND=0
ISSUES_DETAIL=""

# =============================================================================
# 1. gitleaks로 시크릿 스캔
# =============================================================================
if command -v gitleaks &>/dev/null; then
  echo "  도구: gitleaks"
  output=$(gitleaks detect --source . --no-git --exit-code 1 2>&1 || true)
  if echo "$output" | grep -q "leaks found"; then
    ISSUES_FOUND=1
    ISSUES_DETAIL="${ISSUES_DETAIL}\n  [gitleaks]\n$(echo "$output" | sed 's/^/    /')"
  fi
else
  # 폴백: 직접 패턴 매칭
  echo "  도구: 패턴 매칭 (gitleaks 미설치)"

  # 하드코딩된 패스워드/토큰 패턴
  patterns=(
    "password\s*=\s*['\"][^'\"]{8,}"
    "secret\s*=\s*['\"][^'\"]{8,}"
    "api_key\s*=\s*['\"][^'\"]{8,}"
    "private_key\s*=\s*-----BEGIN"
    "-----BEGIN RSA PRIVATE KEY-----"
    "-----BEGIN EC PRIVATE KEY-----"
  )

  for pattern in "${patterns[@]}"; do
    matches=$(grep -rn --include="*.kt" --include="*.java" --include="*.ts" --include="*.py" \
      --include="*.yaml" --include="*.yml" --include="*.json" \
      --exclude-dir=".git" --exclude-dir="node_modules" --exclude-dir="build" \
      --exclude-dir="test" --exclude-dir="androidTest" \
      -E "$pattern" . 2>/dev/null || true)
    if [[ -n "$matches" ]]; then
      ISSUES_FOUND=1
      ISSUES_DETAIL="${ISSUES_DETAIL}\n  패턴: $pattern\n$(echo "$matches" | head -5 | sed 's/^/    /')"
    fi
  done
fi

# =============================================================================
# 2. .env 파일 감지 (git에 추적되거나 스테이징된 .env)
# =============================================================================
# git에 이미 커밋된 .env 파일 확인
tracked_env=$(git ls-files 2>/dev/null | grep -E "\.env$|\.env\." || true)
# 아직 커밋되지 않았지만 스테이징된 .env 파일 확인
staged_env=$(git diff --cached --name-only 2>/dev/null | grep -E "\.env$|\.env\." || true)

env_files="${tracked_env}${tracked_env:+$'\n'}${staged_env}"
env_files=$(echo "$env_files" | sort -u | grep -v '^$' || true)

if [[ -n "$env_files" ]]; then
  ISSUES_FOUND=1
  ISSUES_DETAIL="${ISSUES_DETAIL}\n  [경고] .env 파일이 git에 포함됨:\n$(echo "$env_files" | sed 's/^/    /')"
fi

# =============================================================================
# 결과
# =============================================================================
if [[ $ISSUES_FOUND -ne 0 ]]; then
  cat <<EOF

[SECURITY FAILURE] validators/04-security.sh

  발견된 보안 문제:
$(echo -e "$ISSUES_DETAIL")

  수정 방법:
  1. 하드코딩된 시크릿을 환경 변수 또는 시크릿 관리 도구로 이동하세요
     예: System.getenv("API_KEY"), os.environ["API_KEY"]
  2. .env 파일은 .gitignore에 추가하세요
  3. 이미 커밋된 경우: git history 정리가 필요합니다 (팀에 알리세요)

  참고:
  - .gitignore에 .env* 패턴 추가 권장
  - 시크릿 관리: Vault, AWS Secrets Manager, 환경 변수

EOF
  exit 1
fi

echo "[SECURITY] 보안 검사 통과"
