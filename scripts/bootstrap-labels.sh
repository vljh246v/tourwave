#!/usr/bin/env bash
# bootstrap-labels.sh
#
# 용도: GitHub 리포에 라벨 일괄 생성
# - milestone, area, domain, size, status 라벨 생성
# - 색상 매핑 및 설명 포함
#
# 전제: gh auth status가 OK여야 함
#
# 사용법:
#   ./scripts/bootstrap-labels.sh
#   ./scripts/bootstrap-labels.sh --help

set -euo pipefail

OWNER="vljh246v"
REPO="tourwave"

usage() {
  cat <<EOF
Usage: bootstrap-labels.sh [OPTIONS]

Options:
  --help    Show this help message
  --dry-run Print labels to be created without executing

Creates or updates GitHub labels for task management:
  - Milestone labels (M1, M2, M3, M4, cross)
  - Area labels (BE, FE, SHARED)
  - Domain labels (16 domains)
  - Size labels (S, M, L)
  - Status labels (blocked)

Requires: gh CLI authenticated
EOF
  exit 0
}

DRY_RUN=0
[[ "${1:-}" == "--help" ]] && usage
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=1

# Check gh CLI
if ! command -v gh >/dev/null 2>&1; then
  echo "Error: gh CLI not found. Install from https://cli.github.com/" >&2
  exit 1
fi

# Check authentication
if ! gh auth status >/dev/null 2>&1; then
  echo "Error: Not authenticated to GitHub. Run: gh auth login" >&2
  exit 1
fi

CREATED=0
UPDATED=0

# Helper function to create/update label
create_label() {
  local name="$1"
  local color="$2"
  local description="$3"

  if [[ $DRY_RUN -eq 1 ]]; then
    echo "[DRY-RUN] Would create/update label: $name (#$color)"
    return 0
  fi

  # gh label create will force-update if exists
  if gh label create --repo "$OWNER/$REPO" "$name" \
    --color "$color" \
    --description "$description" \
    --force 2>/dev/null; then
    ((CREATED++))
    echo "[✓] Created/updated: $name"
  else
    echo "[!] Failed to create: $name" >&2
  fi
}

echo "=== Bootstrapping GitHub Labels ==="
echo ""

# Milestone labels (green)
echo "Milestone labels..."
create_label "milestone:M1" "0E8A16" "Milestone M1: Auth & Discovery"
create_label "milestone:M2" "0E8A16" "Milestone M2: Core Booking Flow"
create_label "milestone:M3" "0E8A16" "Milestone M3: Operations & Fulfillment"
create_label "milestone:M4" "0E8A16" "Milestone M4: Reporting & Analytics"
create_label "milestone:cross" "0E8A16" "Cross-cutting concern"

# Area labels (blue)
echo "Area labels..."
create_label "area:BE" "1D76DB" "Backend implementation"
create_label "area:FE" "1D76DB" "Frontend implementation"
create_label "area:SHARED" "1D76DB" "Shared responsibility (SDK, schema, docs)"

# Domain labels (orange)
echo "Domain labels..."
create_label "domain:auth" "D93F0B" "Authentication & Authorization"
create_label "domain:booking" "D93F0B" "Booking & Reservations"
create_label "domain:common" "D93F0B" "Common utilities & shared"
create_label "domain:customer" "D93F0B" "Customer management"
create_label "domain:inquiry" "D93F0B" "Customer inquiries"
create_label "domain:occurrence" "D93F0B" "Tour occurrence & scheduling"
create_label "domain:organization" "D93F0B" "Organization & business"
create_label "domain:instructor" "D93F0B" "Instructor management"
create_label "domain:tour" "D93F0B" "Tour catalog & metadata"
create_label "domain:user" "D93F0B" "User & profile"
create_label "domain:payment" "D93F0B" "Payment processing"
create_label "domain:asset" "D93F0B" "Asset management (images, etc)"
create_label "domain:announcement" "D93F0B" "Announcements & notifications"
create_label "domain:operations" "D93F0B" "Operations & fulfillment"
create_label "domain:participant" "D93F0B" "Participant management"
create_label "domain:review" "D93F0B" "Reviews & ratings"

# Size labels (yellow)
echo "Size labels..."
create_label "size:S" "FBCA04" "Small task (~2 hours)"
create_label "size:M" "FBCA04" "Medium task (~4-8 hours)"
create_label "size:L" "FBCA04" "Large task (~16+ hours)"

# Status labels (red)
echo "Status labels..."
create_label "status:blocked" "B60205" "Task is blocked, cannot proceed"

echo ""
echo "=== Summary ==="
if [[ $DRY_RUN -eq 1 ]]; then
  total_labels=48
  echo "Dry-run: Would create/update $total_labels labels"
else
  echo "Created/updated: $CREATED labels"
fi

exit 0
