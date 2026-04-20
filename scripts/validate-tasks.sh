#!/usr/bin/env bash
# validate-tasks.sh
#
# 용도: 카드 파일들의 정합성 검증 (YAML frontmatter 포맷 기준)
# - 모든 T-*.md 파일의 WRITE 경로 충돌 검사
# - 필수 섹션 존재 여부 확인 (Verification, SSOT 근거)
# - 필수 frontmatter 필드 존재 여부 확인 (id, milestone, area, size)
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
  - Required sections (Verification, SSOT 근거)
  - Required frontmatter fields (id, milestone, area, size)

Returns 0 if all checks pass, 1 if issues found.
EOF
  exit 0
}

VERBOSE=0
[[ "${1:-}" == "--help" ]] && usage
[[ "${1:-}" == "--verbose" ]] && VERBOSE=1

CARDS=()
while IFS= read -r card; do
  CARDS+=("$card")
done < <(find "${DOCS_DIR}" -name "T-*.md" -type f | sort)

if [[ ${#CARDS[@]} -eq 0 ]]; then
  echo "No task cards found in ${DOCS_DIR}" >&2
  exit 1
fi

TOTAL_CARDS=${#CARDS[@]}

WRITE_PATHS=$(mktemp)
CONFLICTS_FILE=$(mktemp)
trap "rm -f '$WRITE_PATHS' '$CONFLICTS_FILE'" EXIT

# Extract WRITE section paths from body
for card_file in "${CARDS[@]}"; do
  card_id=$(basename "$card_file" .md)
  in_write=0
  while IFS= read -r line; do
    if [[ "$line" =~ ^WRITE:$ ]]; then
      in_write=1
      continue
    fi
    if [[ $in_write -eq 1 ]]; then
      if [[ "$line" =~ ^[^[:space:]] ]] && [[ ! "$line" =~ ^[[:space:]] ]]; then
        break
      fi
      if [[ "$line" =~ ^\s+-\s+\`([^\`]+)\` ]]; then
        path="${BASH_REMATCH[1]}"
        echo "$path|$card_id" >> "$WRITE_PATHS"
      fi
    fi
  done < "$card_file"
done

sort "$WRITE_PATHS" | uniq -d | while read -r dup_line; do
  path="${dup_line%|*}"
  cards=$(grep "^${path}\|" "$WRITE_PATHS" | sed 's/.*|//' | tr '\n' ' ')
  echo "$path: $cards" >> "$CONFLICTS_FILE"
done

# frontmatter 검사: file starts with --- and has required fields before next ---
has_frontmatter_field() {
  local file="$1" field="$2"
  awk -v f="$field" '
    NR == 1 && /^---$/ { in_fm=1; next }
    in_fm && /^---$/ { exit }
    in_fm && $0 ~ "^" f ":" { found=1; exit }
    END { exit found ? 0 : 1 }
  ' "$file"
}

for card_file in "${CARDS[@]}"; do
  card_id=$(basename "$card_file" .md)
  issues=0

  # frontmatter 존재 확인
  if [[ "$(head -1 "$card_file")" != "---" ]]; then
    echo "[$card_id] Missing YAML frontmatter (first line != '---')" >&2
    ((issues++))
    ((MISSING_SECTIONS += issues))
    continue
  fi

  # 필수 frontmatter 필드
  for field in id milestone area size; do
    if ! has_frontmatter_field "$card_file" "$field"; then
      echo "[$card_id] Missing frontmatter field: $field" >&2
      ((issues++))
    fi
  done

  # 필수 섹션
  if ! grep -q "^## Verification$" "$card_file"; then
    echo "[$card_id] Missing 'Verification' section" >&2
    ((issues++))
  fi

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

if [[ -f "$CONFLICTS_FILE" ]]; then
  CONFLICTS=$(wc -l < "$CONFLICTS_FILE")
fi

if [[ $CONFLICTS -gt 0 ]]; then
  echo "" >&2
  echo "=== WRITE Path Conflicts ===" >&2
  if [[ -f "$CONFLICTS_FILE" ]]; then
    cat "$CONFLICTS_FILE" >&2
  fi
  echo "" >&2
fi

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
