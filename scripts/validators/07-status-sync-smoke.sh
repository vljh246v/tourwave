#!/usr/bin/env bash
# 07-status-sync-smoke.sh
# task-status-sync.py 기본 동작 smoke 테스트.
# 임시 디렉토리에 샘플 카드를 만들고 set/reconcile 을 돌려 기대 결과 검증.
set -euo pipefail

WORKTREE="${1:-}"
PROJECT_ROOT="${2:-}"

if [[ -z "$PROJECT_ROOT" ]]; then
  PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
fi

SYNC="$PROJECT_ROOT/scripts/task-status-sync.py"
if [[ ! -f "$SYNC" ]]; then
  echo "task-status-sync.py not found at $SYNC" >&2
  exit 1
fi

TMPDIR=$(mktemp -d)
trap "rm -rf $TMPDIR" EXIT

mkdir -p "$TMPDIR/docs/tasks/M1"
cat > "$TMPDIR/docs/tasks/M1/T-999-smoke.md" <<'EOF'
---
id: T-999
status: backlog
updated: 2026-01-01
---

#status/backlog #area/be

# T-999
EOF

# set backlog → in-progress
python3 "$SYNC" set --task-id T-999 --status in-progress --repo-root "$TMPDIR" > /dev/null
grep -q "^status: in-progress" "$TMPDIR/docs/tasks/M1/T-999-smoke.md" \
  || { echo "FAIL: status not patched"; exit 1; }
grep -q "^#status/in-progress " "$TMPDIR/docs/tasks/M1/T-999-smoke.md" \
  || { echo "FAIL: tag not patched"; exit 1; }

# 멱등성
out=$(python3 "$SYNC" set --task-id T-999 --status in-progress --repo-root "$TMPDIR")
echo "$out" | grep -q NOOP \
  || { echo "FAIL: idempotent re-run did not report NOOP"; exit 1; }

# 없는 task-id
if python3 "$SYNC" set --task-id T-404 --status done --repo-root "$TMPDIR" 2>/dev/null; then
  echo "FAIL: missing task-id should exit 1"
  exit 1
fi

echo "07-status-sync-smoke: PASS"
