#!/usr/bin/env bash
# publish-tasks.sh
#
# 용도: 카드들을 GitHub Issue로 발행 + Projects v2 등록
# - 카드 파일을 GitHub Issue로 생성
# - 라벨 및 메타데이터 자동 적용
# - GitHub Projects v2에 항목 추가
#
# 사용법:
#   ./scripts/publish-tasks.sh [MILESTONE] [--dry-run]
#   ./scripts/publish-tasks.sh M1
#   ./scripts/publish-tasks.sh all --dry-run
#   ./scripts/publish-tasks.sh --help

set -euo pipefail

OWNER="vljh246v"
REPO="tourwave"
DOCS_DIR="${DOCS_DIR:-.}/docs/tasks"

usage() {
  cat <<EOF
Usage: publish-tasks.sh [MILESTONE] [OPTIONS]

Arguments:
  MILESTONE    Milestone filter: M1, M2, M3, M4, cross, or all (default: all)

Options:
  --help       Show this help message
  --dry-run    Print what would be created without executing

Creates GitHub Issues from task cards and adds them to Projects v2.

Requires:
  - gh CLI authenticated
  - jq for JSON parsing
  - TOURWAVE_PROJECT_NUMBER environment variable set
EOF
  exit 0
}

DRY_RUN=0
MILESTONE="${1:-all}"

[[ "${1:-}" == "--help" ]] && usage
[[ "${MILESTONE}" == "--help" ]] && usage

if [[ "${2:-}" == "--dry-run" ]]; then
  DRY_RUN=1
elif [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=1
  MILESTONE="all"
fi

# Check dependencies
for cmd in gh jq; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Error: $cmd not found" >&2
    exit 1
  fi
done

# Check authentication
if ! gh auth status >/dev/null 2>&1; then
  echo "Error: Not authenticated to GitHub. Run: gh auth login" >&2
  exit 1
fi

# Check project environment (optional - skip project add if not set)
if [[ $DRY_RUN -eq 0 ]] && [[ -z "${TOURWAVE_PROJECT_NUMBER:-}" ]]; then
  echo "Warning: TOURWAVE_PROJECT_NUMBER environment variable not set" >&2
  echo "Issues will be created but not added to Projects v2" >&2
fi

PUBLISHED=0
SKIPPED=0
ERRORS=0
TEMP_PUBLISHED=0
TEMP_SKIPPED=0
TEMP_ERRORS=0

# Helper: Extract a field value from YAML frontmatter (--- ... ---)
# Usage: frontmatter_get_field <field> <file>
# Returns: field value with leading/trailing whitespace and surrounding quotes stripped
# Returns empty string if field not found or file has no frontmatter
frontmatter_get_field() {
  local field="$1"
  local file="$2"
  awk -v f="$field" '
    NR == 1 && /^---$/ { in_fm=1; next }
    in_fm && /^---$/ { exit }
    in_fm && $0 ~ ("^" f ":") {
      val = $0
      sub("^" f ":[[:space:]]*", "", val)
      # Strip surrounding quotes if present
      gsub(/^["'"'"']|["'"'"']$/, "", val)
      # Strip trailing whitespace
      gsub(/[[:space:]]+$/, "", val)
      print val
      exit
    }
  ' "$file"
}

# Helper: Create GitHub issue from card
publish_card() {
  local card_file="$1"
  local card_id
  card_id=$(basename "$card_file" .md)

  # Extract title from frontmatter `title:` field
  # Fallback: first `# ` heading after frontmatter
  local title
  title=$(frontmatter_get_field "title" "$card_file")
  if [[ -z "$title" ]]; then
    # Fallback: find first # heading after frontmatter
    title=$(awk '
      NR == 1 && /^---$/ { in_fm=1; next }
      in_fm && /^---$/ { in_fm=0; next }
      !in_fm && /^# / { sub(/^# /, ""); print; exit }
    ' "$card_file")
  fi
  # Normalize em-dash separator for GitHub issue title
  title=$(echo "$title" | sed 's/ — /: /')

  # Extract metadata from frontmatter
  local milestone
  milestone=$(frontmatter_get_field "milestone" "$card_file")
  local domain
  domain=$(frontmatter_get_field "domain" "$card_file")
  local area
  area=$(frontmatter_get_field "area" "$card_file")
  local size
  size=$(frontmatter_get_field "size" "$card_file")

  # Check if already published: github_issue field is a number (not null / empty / missing)
  local current_issue
  current_issue=$(frontmatter_get_field "github_issue" "$card_file")
  if [[ -n "$current_issue" ]] && [[ "$current_issue" != "null" ]] && [[ "$current_issue" =~ ^[0-9]+$ ]]; then
    echo "[skip] $card_id: Already published as #$current_issue"
    ((TEMP_SKIPPED++))
    return 0
  fi

  # Build labels
  # Handle cross milestone variations (cross / Cross-cutting)
  local milestone_label="$milestone"
  if [[ "$milestone" == "Cross-cutting" ]] || [[ "$milestone" == "cross" ]]; then
    milestone_label="cross"
  fi
  # Sanitize domain: replace spaces with hyphens, shorten long names
  local domain_label
  domain_label=$(echo "$domain" | sed 's/ /-/g')
  # Special shortening for long domains
  if [[ "$domain_label" == "API-Layer" ]]; then
    domain_label="api"
  fi
  local labels="milestone:$milestone_label,area:$area,domain:$domain_label,size:$size"

  if [[ $DRY_RUN -eq 1 ]]; then
    echo "[dry-run] Would create issue: $card_id"
    echo "  Title: $title"
    echo "  Labels: $labels"
    ((TEMP_PUBLISHED++))
    return 0
  fi

  # Create issue
  local issue_url
  if issue_url=$(gh issue create --repo "$OWNER/$REPO" \
    --title "$title" \
    --body-file "$card_file" \
    --label "$labels" 2>&1); then

    # Extract issue number from URL
    local issue_num
    issue_num=$(echo "$issue_url" | grep -oE '[0-9]+$')

    # Update card file: replace `github_issue: null` with `github_issue: <num>`
    if sed -i '' "s/^github_issue: null$/github_issue: $issue_num/" "$card_file"; then
      echo "[v] $card_id: Created #$issue_num"
      ((TEMP_PUBLISHED++))

      # Add to Projects v2 (if PROJECT_NUMBER is set)
      if [[ -n "${TOURWAVE_PROJECT_NUMBER:-}" ]]; then
        if ! add_to_project "$issue_url" "$milestone" "$domain" "$area" "$size"; then
          echo "[!] $card_id: Failed to add to project" >&2
          ((TEMP_ERRORS++))
        fi
      fi
    else
      echo "[!] $card_id: Failed to update card with issue number" >&2
      ((TEMP_ERRORS++))
    fi
  else
    echo "[!] $card_id: Failed to create issue" >&2
    echo "   Error: $issue_url" >&2
    ((TEMP_ERRORS++))
  fi
}

# Helper: Add issue to Projects v2
add_to_project() {
  local issue_url="$1"
  local milestone="$2"
  local domain="$3"
  local area="$4"
  local size="$5"

  # Add item to project
  if ! gh project item-add "$TOURWAVE_PROJECT_NUMBER" \
    --owner "$OWNER" \
    --url "$issue_url" 2>&1 >/dev/null; then
    return 1
  fi

  # Note: Setting custom fields requires field ID mapping which is complex
  # This is a simplified version. Full implementation would need:
  # 1. Query project fields: gh project field-list --owner vljh246v <PROJECT_NUM> --format json
  # 2. Parse field IDs
  # 3. Use gh project item-edit to set Status, Milestone, etc.
  # For now, the issue is added but fields would need manual assignment or a separate step

  return 0
}

# Filter and process cards
echo "=== Publishing Task Cards ==="
echo "Milestone filter: $MILESTONE"
echo "Dry-run: $([ $DRY_RUN -eq 1 ] && echo 'YES' || echo 'NO')"
echo ""

# Use find for bash 3.2 compatibility (no arrays)
# Avoid pipe to while to preserve variable scope
while IFS= read -r card_file; do
  [[ -z "$card_file" ]] && continue

  # Apply milestone filter using frontmatter field
  if [[ "$MILESTONE" != "all" ]]; then
    card_milestone=$(frontmatter_get_field "milestone" "$card_file")
    if [[ "$MILESTONE" == "cross" ]]; then
      # Match both "cross" and "Cross-cutting" frontmatter values
      if [[ "$card_milestone" != "cross" ]] && [[ "$card_milestone" != "Cross-cutting" ]]; then
        continue
      fi
    else
      if [[ "$card_milestone" != "$MILESTONE" ]]; then
        continue
      fi
    fi
  fi

  publish_card "$card_file"
done < <(find "${DOCS_DIR}" -name "T-*.md" -type f | sort)

PUBLISHED=$TEMP_PUBLISHED
SKIPPED=$TEMP_SKIPPED
ERRORS=$TEMP_ERRORS

echo ""
echo "=== Summary ==="
echo "Published: $PUBLISHED"
echo "Skipped: $SKIPPED"
echo "Errors: $ERRORS"

if [[ $ERRORS -gt 0 ]]; then
  exit 1
else
  exit 0
fi
