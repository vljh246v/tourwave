#!/usr/bin/env bash
# 08-propagate-smoke.sh — task-status-sync.py propagate/reviewed 격리 smoke.
# stdlib 격리 repo 2개 (BE 역할, cards 역할) 생성 → drift 탐지 → reviewed bump → clear.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SYNC="$ROOT/scripts/task-status-sync.py"

if [[ ! -x "$SYNC" ]]; then
  echo "[08-smoke] task-status-sync.py 실행 불가 — skip"
  exit 0
fi

WORK="$(mktemp -d -t tw-08-smoke-XXXXXX)"
trap 'rm -rf "$WORK"' EXIT

# BE 역할 repo
BE="$WORK/be"; mkdir -p "$BE/docs"
(
  cd "$BE"
  git init -q
  git config user.email "smoke@t" && git config user.name "smoke"
  echo "openapi: 3.0" > docs/openapi.yaml
  git add . && \
    GIT_COMMITTER_DATE="2026-01-01T00:00:00Z" GIT_AUTHOR_DATE="2026-01-01T00:00:00Z" \
    git commit -q -m "initial"
  echo "# updated" >> docs/openapi.yaml
  git add . && \
    GIT_COMMITTER_DATE="2026-04-10T00:00:00Z" GIT_AUTHOR_DATE="2026-04-10T00:00:00Z" \
    git commit -q -m "openapi update"
)

# Cards 역할 repo (BE 와 다른 디렉토리)
CARDS="$WORK/cards"; mkdir -p "$CARDS/docs/tasks/M1"
(
  cd "$CARDS"
  git init -q
  git config user.email "smoke@t" && git config user.name "smoke"
)

cat > "$CARDS/docs/tasks/M1/T-300-smoke.md" <<'CARD'
---
id: T-300
title: "smoke card"
aliases: [T-300]
repo: smoke
area: fe
milestone: M1
domain: infra
layer: infra
size: XS
status: backlog
depends_on: []
blocks: []
sub_tasks: []
github_issue: null
exec_plan: ""
ssot_refs: [docs/openapi.yaml]
reviewed: "2026-01-01"
stale_refs: []
created: 2026-01-01
updated: 2026-01-01
---

#status/backlog #area/fe
body
CARD

# propagate --apply: stale_refs 채움
python3 "$SYNC" propagate --apply --repo-root "$CARDS" --be-root "$BE" >/dev/null
if ! grep -q "source: docs/openapi.yaml" "$CARDS/docs/tasks/M1/T-300-smoke.md"; then
  echo "[08-smoke] FAIL — propagate 가 stale_refs 에 ssot_drift 엔트리를 쓰지 못함"
  exit 1
fi

# reviewed 서브커맨드: baseline 갱신
python3 "$SYNC" reviewed T-300 --date 2026-04-20 --repo-root "$CARDS" >/dev/null

# propagate --apply 재실행: stale_refs 비워져야 함
python3 "$SYNC" propagate --apply --repo-root "$CARDS" --be-root "$BE" >/dev/null
if ! grep -q "^stale_refs: \[\]$" "$CARDS/docs/tasks/M1/T-300-smoke.md"; then
  echo "[08-smoke] FAIL — reviewed bump 후 stale_refs 가 비워지지 않음"
  sed -n '1,25p' "$CARDS/docs/tasks/M1/T-300-smoke.md"
  exit 1
fi

echo "[08-smoke] PASS"
