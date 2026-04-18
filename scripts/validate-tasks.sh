#!/usr/bin/env bash
# validate-tasks.sh
#
# 용도: 카드 파일들의 정합성 검증
# - 모든 T-*.md 파일의 WRITE 경로 충돌 검사
# - 필수 섹션 존재 여부 확인 (Meta, Verification, SSOT 근거)
# - 필수 필드 (ID, Milestone, Area, Size) 존재 여부 확인
#
# 사용법:
#   ./scripts/validate-tasks.sh
#   ./scripts/validate-tasks.sh --help

set -euo pipefail

DOCS_DIR="${DOCS_DIR:-.}/docs/tasks"
TOTAL_CARDS=0
CONFLICTS=0
MISSING_SECTIONS=0
PASS_COUNT=0

usage() {
  cat <<EOF
Usage: validate-tasks.sh [OPTIONS]

Options:
  --help    Show this help message
  --verbose Show detailed conflict information

Validates task card consistency:
  - WRITE path conflicts between cards
  - Required sections (Meta, Verification, SSOT 근거)
  - Required Meta fields (ID, Milestone, Area, Size)

Returns 0 if all checks pass, 1 if issues found.
EOF
  exit 0
}

VERBOSE=0
[[ "${1:-}" == "--help" ]] && usage
[[ "${1:-}" == "--verbose" ]] && VERBOSE=1

# Find all task cards
CARDS=()
while IFS= read -r card; do
  CARDS+=("$card")
done < <(find "${DOCS_DIR}" -name "T-*.md" -type f | sort)

if [[ ${#CARDS[@]} -eq 0 ]]; then
  echo "No task cards found in ${DOCS_DIR}" >&2
  exit 1
fi

TOTAL_CARDS=${#CARDS[@]}

# Temporary files
WRITE_PATHS=$(mktemp)
CONFLICTS_FILE=$(mktemp)
trap "rm -f '$WRITE_PATHS' '$CONFLICTS_FILE'" EXIT

# Parse WRITE sections from each card and build list
for card_file in "${CARDS[@]}"; do
  card_id=$(basename "$card_file" .md)

  # Extract WRITE block (indented list under WRITE:)
  # Look for lines starting with WRITE: and grab indented bullet points until next section
  in_write=0
  while IFS= read -r line; do
    if [[ "$line" =~ ^WRITE:$ ]]; then
      in_write=1
      continue
    fi

    if [[ $in_write -eq 1 ]]; then
      # Check if we've hit the next section (line starting with ##, #, or non-indented line)
      if [[ "$line" =~ ^[^[:space:]] ]] && [[ ! "$line" =~ ^[[:space:]] ]]; then
        break
      fi

      # Extract path from `...` format (indented bullet)
      if [[ "$line" =~ ^\s+-\s+\`([^\`]+)\` ]]; then
        path="${BASH_REMATCH[1]}"
        echo "$path|$card_id" >> "$WRITE_PATHS"
      fi
    fi
  done < "$card_file"
done

# Check for conflicts (paths appearing multiple times)
sort "$WRITE_PATHS" | uniq -d | while read -r dup_line; do
  path="${dup_line%|*}"
  # Find all cards with this path
  cards=$(grep "^${path}\|" "$WRITE_PATHS" | sed 's/.*|//' | tr '\n' ' ')
  echo "$path: $cards" >> "$CONFLICTS_FILE"
  ((CONFLICTS++))
done

# Check required sections and fields
for card_file in "${CARDS[@]}"; do
  card_id=$(basename "$card_file" .md)
  issues=0

  # Check Meta section
  if ! grep -q "^## Meta$" "$card_file"; then
    echo "[$card_id] Missing 'Meta' section" >&2
    ((issues++))
  fi

  # Check required Meta fields
  for field in "ID:" "Milestone:" "Area:" "Size:"; do
    if ! grep -q "^- $field" "$card_file"; then
      echo "[$card_id] Missing Meta field: $field" >&2
      ((issues++))
    fi
  done

  # Check Verification section
  if ! grep -q "^## Verification$" "$card_file"; then
    echo "[$card_id] Missing 'Verification' section" >&2
    ((issues++))
  fi

  # Check SSOT 근거 section
  if ! grep -q "^## SSOT 근거$" "$card_file"; then
    echo "[$card_id] Missing 'SSOT 근거' section" >&2
    ((issues++))
  fi

  if [[ $issues -gt 0 ]]; then
    ((MISSING_SECTIONS += issues))
  else
    ((PASS_COUNT++))
  fi
done

# Count actual conflicts
if [[ -f "$CONFLICTS_FILE" ]]; then
  CONFLICTS=$(wc -l < "$CONFLICTS_FILE")
fi

# Print conflicts summary
if [[ $CONFLICTS -gt 0 ]]; then
  echo "" >&2
  echo "=== WRITE Path Conflicts ===" >&2
  if [[ -f "$CONFLICTS_FILE" ]]; then
    cat "$CONFLICTS_FILE" >&2
  fi
  echo "" >&2
fi

# Final summary
echo ""
echo "=== Summary ==="
echo "Total cards: $TOTAL_CARDS"
echo "Conflicts: $CONFLICTS"
echo "Missing sections: $MISSING_SECTIONS"
echo "Passed: $PASS_COUNT"

if [[ $CONFLICTS -gt 0 ]] || [[ $MISSING_SECTIONS -gt 0 ]]; then
  echo "Status: FAIL" >&2
  exit 1
else
  echo "Status: PASS"
  exit 0
fi
