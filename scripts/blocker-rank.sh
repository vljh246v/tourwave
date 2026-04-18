#!/usr/bin/env bash
# blocker-rank.sh — 블록커 영향력 순위
#
# 사용법:
#   ./scripts/blocker-rank.sh              # 전체 순위
#   ./scripts/blocker-rank.sh --top 10     # 상위 N개
#   ./scripts/blocker-rank.sh T-010        # T-010이 블로킹하는 카드 목록

cd "$(dirname "$0")/.."

# 개별 카드 조회 모드
if [ $# -ge 1 ] && echo "$1" | grep -qE '^T-[0-9]+$'; then
  tid="$1"
  echo "=== $tid 가 블로킹하는 카드 ==="
  find docs/tasks -type f -name 'T-*.md' \
    -exec grep -lE "^- Depends on:.*$tid" {} \; 2>/dev/null \
    | sort -u \
    | while read -r f; do
        head -1 "$f" | sed 's/^#* *//' | awk '{ print "  " $0 }'
      done
  exit 0
fi

# 순위 모드
TOP=${TOP:-999}
if [ "${1:-}" = "--top" ] && [ -n "${2:-}" ]; then TOP="$2"; fi

echo "=== 블록커 영향력 순위 (내림차순, 상위 $TOP) ==="

find docs/tasks -type f -name 'T-*.md' \
  | xargs -I{} basename {} .md \
  | sed -E 's/^(T-[0-9]+).*/\1/' \
  | sort -u \
  | while read -r tid; do
      count=$(find docs/tasks -type f -name 'T-*.md' \
        -exec grep -lE "^- Depends on:.*$tid" {} \; 2>/dev/null \
        | sort -u | wc -l | tr -d ' ')
      [ "$count" -eq 0 ] && continue
      title=$(find docs/tasks -type f -name 'T-*.md' \
        -exec grep -l "^- ID: $tid\$" {} \; 2>/dev/null | head -1 \
        | xargs head -1 2>/dev/null | sed 's/^#* *//')
      printf "%3d  %s\n" "$count" "$title"
    done \
  | sort -rn \
  | head -n "$TOP"
