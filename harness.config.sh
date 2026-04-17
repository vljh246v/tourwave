#!/usr/bin/env bash
# =============================================================================
# harness.config.sh — Tourwave 하네스 설정
# =============================================================================

PROJECT_NAME="tourwave"

# =============================================================================
# Git Flow 설정
# =============================================================================

GIT_REMOTE="origin"
MERGE_STRATEGY="direct"           # "pr" = push 후 PR 생성

PHASE_DEVELOP_BASE="develop"
PHASE_RELEASE_BASE=""          # 매번 물어봄
PHASE_HOTFIX_BASE="main"

BASE_BRANCH="develop"          # 기본값 (harness-task가 단계에 따라 덮어씀)

# =============================================================================
# 브랜치 & 커밋 규칙
# =============================================================================

BRANCH_PREFIX="feature"        # feature/<task-id>

# Conventional Commits
# 예: "feat: TOUR-101 예약 취소 API 추가"
COMMIT_TEMPLATE="<type>: <task-id> <요약>"

PR_TITLE_TEMPLATE="<type>: <task-id> <요약>"
# PR_REVIEWERS=""
# PR_LABELS=""

WORKTREE_ROOT=".worktrees"
LOG_DIR="./logs"

# =============================================================================
# 빌드/테스트/린트 커맨드
# =============================================================================

BUILD_CMD="./gradlew build -x test"
TEST_CMD="./gradlew test"
# detekt/ktlint 미설정 — 컴파일 검증으로 대체
# TODO: build.gradle.kts에 detekt 또는 ktlint 플러그인 추가 후 교체
LINT_CMD="./gradlew compileTestKotlin --console=plain"

# =============================================================================
# 보안 스캔
# =============================================================================

SECURITY_SCAN_CMD="${SECURITY_SCAN_CMD:-gitleaks detect --source . --no-git 2>/dev/null || true}"

# =============================================================================
# 검증기 활성화 여부
# =============================================================================
ENABLE_BUILD_CHECK="${ENABLE_BUILD_CHECK:-true}"
ENABLE_TEST_CHECK="${ENABLE_TEST_CHECK:-true}"
ENABLE_LINT_CHECK="${ENABLE_LINT_CHECK:-true}"
ENABLE_SECURITY_CHECK="${ENABLE_SECURITY_CHECK:-true}"
ENABLE_DOCS_CHECK="${ENABLE_DOCS_CHECK:-true}"

ARCHITECTURE_DOC="docs/architecture.md"
