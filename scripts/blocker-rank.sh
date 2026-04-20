#!/usr/bin/env bash
# blocker-rank.sh — 블록커 영향력 순위 (Done 카드 제외)
# YAML frontmatter 포맷 기반
#
# 사용법:
#   ./scripts/blocker-rank.sh              # 전체 순위
#   ./scripts/blocker-rank.sh --top 10     # 상위 N개
#   ./scripts/blocker-rank.sh T-010        # T-010이 블로킹하는 카드 목록

cd "$(dirname "$0")/.."

# frontmatter 에서 status: done 인지 검사 (대소문자 무시)
is_done() {
  awk '
    NR == 1 && /^---$/ { in_fm=1; next }
    in_fm && /^---$/ { exit }
    in_fm && /^status:[[:space:]]*[Dd][Oo][Nn][Ee]/ { found=1; exit }
    END { exit found ? 0 : 1 }
  ' "$1" 2>/dev/null
}

# depends_on 배열에 특정 T-ID 가 포함되는지 검사
# YAML depends_on: ['T-010', 'T-005'] 또는 depends_on: [T-010, T-005] 모두 지원
has_dependency() {
  local file="$1" tid="$2"
  awk -v tid="$tid" '
    NR == 1 && /^---$/ { in_fm=1; next }
    in_fm && /^---$/ { exit }
    in_fm && /^depends_on:/ {
      # 한 줄 배열 형태만 파싱 (migrate-tasks.py 출력 형식)
      if (match($0, /\[.*\]/)) {
        arr = substr($0, RSTART, RLENGTH)
        if (match(arr, tid)) { found=1 }
      }
    }
    END { exit found ? 0 : 1 }
  ' "$file" 2>/dev/null
}

# frontmatter 에서 id 추출
get_id() {
  awk '
    NR == 1 && /^---$/ { in_fm=1; next }
    in_fm && /^---$/ { exit }
    in_fm && /^id:[[:space:]]*/ {
      sub(/^id:[[:space:]]*/, "")
      gsub(/[[:space:]]+$/, "")
      print
      exit
    }
  ' "$1"
}

# 카드 파일에서 title (첫 H1 또는 frontmatter title 필드) 추출
get_title() {
  local file="$1"
  # frontmatter title 필드 선호
  local t
  t=$(awk '
    NR == 1 && /^---$/ { in_fm=1; next }
    in_fm && /^---$/ { exit }
    in_fm && /^title:[[:space:]]*/ {
      sub(/^title:[[:space:]]*"?/, "")
      sub(/"[[:space:]]*$/, "")
      print
      exit
    }
  ' "$file")
  if [ -n "$t" ]; then
    echo "$t"
  else
    head -1 "$file" | sed 's/^#* *//'
  fi
}

# 개별 카드 조회 모드
if [ $# -ge 1 ] && echo "$1" | grep -qE '^T-[0-9]+$'; then
  tid="$1"
  echo "=== $tid 가 블로킹하는 카드 (미완료만) ==="
  find docs/tasks -type f -name 'T-*.md' | sort -u | while read -r f; do
    has_dependency "$f" "$tid" || continue
    is_done "$f" && continue
    get_title "$f" | awk '{ print "  " $0 }'
  done
  exit 0
fi

# 순위 모드
TOP=${TOP:-999}
if [ "${1:-}" = "--top" ] && [ -n "${2:-}" ]; then TOP="$2"; fi

echo "=== 블록커 영향력 순위 — 미완료 카드 기준 (상위 $TOP) ==="

find docs/tasks -type f -name 'T-*.md' \
  | xargs -I{} basename {} .md \
  | sed -E 's/^(T-[0-9]+).*/\1/' \
  | sort -u \
  | while read -r tid; do
      card_file=$(find docs/tasks -type f -name "${tid}-*.md" | head -1)
      [ -z "$card_file" ] && continue
      is_done "$card_file" && continue

      # 미완료 카드 중 이 T-ID에 의존하는 수
      count=0
      while IFS= read -r f; do
        has_dependency "$f" "$tid" || continue
        is_done "$f" && continue
        count=$((count + 1))
      done < <(find docs/tasks -type f -name 'T-*.md')

      [ "$count" -eq 0 ] && continue
      title=$(get_title "$card_file")
      printf "%3d  %s\n" "$count" "$title"
    done \
  | sort -rn \
  | head -n "$TOP"
