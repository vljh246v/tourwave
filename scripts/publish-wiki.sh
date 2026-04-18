#!/usr/bin/env bash
# publish-wiki.sh
#
# 용도: docs/ 마크다운을 vljh246v/tourwave 위키로 sync
# - 자동 파일명 변환 (GitHub Wiki 규칙: 슬래시 불가)
# - Home/_Sidebar/How-It-Works/Milestone-M*.md 자동 생성
#
# 사용법:
#   ./scripts/publish-wiki.sh              # 실제 push
#   ./scripts/publish-wiki.sh --dry-run    # push 없이 복사만
#   ./scripts/publish-wiki.sh --help

set -euo pipefail

OWNER="vljh246v"
REPO="tourwave"
DOCS_DIR="${DOCS_DIR:-.}/docs"
TASKS_DIR="$DOCS_DIR/tasks"
WIKI_REPO="https://github.com/${OWNER}/${REPO}.wiki.git"

DRY_RUN=0

usage() {
  cat <<EOF
Usage: publish-wiki.sh [OPTIONS]

Options:
  --help    Show this help
  --dry-run Copy files without committing/pushing

Requires:
  - Wiki activated (first page exists)
  - HTTPS git access to wiki (uses gh credential helper)
EOF
}

for arg in "$@"; do
  case "$arg" in
    --help) usage; exit 0 ;;
    --dry-run) DRY_RUN=1 ;;
    *) echo "Unknown arg: $arg" >&2; usage; exit 1 ;;
  esac
done

WIKI_DIR=$(mktemp -d)
trap 'rm -rf "$WIKI_DIR"' EXIT

echo "=== Publishing Wiki ==="
echo "Wiki work dir: $WIKI_DIR"
echo "Dry run: $DRY_RUN"
echo ""

# --- Helpers (defined before main body) ---

extract_task_num() {
  # T-042-foo-bar -> 042
  local id="$1"
  echo "$id" | sed -E 's/^T-0*([0-9]+).*/\1/' | awk '{ printf "%03d\n", $1 }'
}

card_milestone() {
  local file="$1"
  grep -m1 '^- Milestone:' "$file" 2>/dev/null | sed -E 's/^- Milestone: *([A-Za-z0-9-]+).*/\1/' || true
}

card_title_line() {
  head -1 "$1" | sed 's/^# *//'
}

card_size() {
  grep -m1 '^- Size:' "$1" 2>/dev/null | sed -E 's/^- Size: *([A-Za-z]+).*/\1/' || echo "?"
}

card_area() {
  grep -m1 '^- Area:' "$1" 2>/dev/null | sed -E 's/^- Area: *([A-Za-z]+).*/\1/' || echo "?"
}

card_domain() {
  grep -m1 '^- Domain:' "$1" 2>/dev/null | sed -E 's/^- Domain: *(.*)/\1/' || echo "?"
}

# --- Step 1: Clone wiki ---

echo "[1/6] Cloning wiki repo..."
if ! git clone "$WIKI_REPO" "$WIKI_DIR" 2>&1; then
  echo "Error: wiki clone failed" >&2
  echo "Make sure wiki is initialized (Settings → Features → Wikis, then create first page)." >&2
  exit 1
fi

TRANSFORMED=0

# --- Step 2: Copy audit files ---

echo "[2/6] Copying audit files..."
if [ -d "$DOCS_DIR/audit" ]; then
  for file in "$DOCS_DIR/audit"/*.md; do
    [ -f "$file" ] || continue
    base=$(basename "$file")
    if [ "$base" = "_index.md" ]; then
      cp "$file" "$WIKI_DIR/Audit.md"
    else
      cp "$file" "$WIKI_DIR/Audit-${base}"
    fi
    TRANSFORMED=$((TRANSFORMED + 1))
  done
fi

# --- Step 3: Copy gap matrix ---

echo "[3/6] Copying gap matrix..."
if [ -f "$DOCS_DIR/gap-matrix.md" ]; then
  cp "$DOCS_DIR/gap-matrix.md" "$WIKI_DIR/Gap-Matrix.md"
  TRANSFORMED=$((TRANSFORMED + 1))
fi

# --- Step 4: Copy task cards + collect per-milestone rows ---

echo "[4/6] Copying task cards + preparing milestone pages..."

ROWS_DIR=$(mktemp -d)
trap 'rm -rf "$WIKI_DIR" "$ROWS_DIR"' EXIT

if [ -d "$TASKS_DIR" ]; then
  find "$TASKS_DIR" -type f -name 'T-*.md' | sort | while read -r card_file; do
    base=$(basename "$card_file" .md)
    # base = T-042-foo-bar
    num=$(extract_task_num "$base")
    out="$WIKI_DIR/Task-T-${num}.md"
    cp "$card_file" "$out"

    ms=$(card_milestone "$card_file")
    [ -z "$ms" ] && ms="unsorted"

    title=$(card_title_line "$card_file")
    size=$(card_size "$card_file")
    area=$(card_area "$card_file")
    domain=$(card_domain "$card_file")

    safe_ms=$(echo "$ms" | tr '/ ' '__')
    row="| [T-${num}](Task-T-${num}) | ${title} | ${size} | ${area} | ${domain} |"
    printf '%s\n' "$row" >> "$ROWS_DIR/${safe_ms}.rows"
  done

  # count
  TRANSFORMED=$((TRANSFORMED + $(find "$WIKI_DIR" -maxdepth 1 -name 'Task-T-*.md' | wc -l | tr -d ' ')))
fi

# --- Step 5: Generate Milestone overview pages ---

echo "[5/6] Generating Milestone-*.md..."
if [ -d "$ROWS_DIR" ]; then
  for rows_file in "$ROWS_DIR"/*.rows; do
    [ -f "$rows_file" ] || continue
    ms=$(basename "$rows_file" .rows)
    # Normalize: M1, M2, ..., Cross-cutting, unsorted
    case "$ms" in
      M1) page="Milestone-M1"; header="# M1 — 인증·탐색" ;;
      M2) page="Milestone-M2"; header="# M2 — 예약·결제" ;;
      M3) page="Milestone-M3"; header="# M3 — 운영·CS" ;;
      M4) page="Milestone-M4"; header="# M4 — 리포팅·정책" ;;
      Cross-cutting) page="Milestone-Cross"; header="# Cross-cutting — 인프라·정책" ;;
      *) page="Milestone-${ms}"; header="# $ms" ;;
    esac
    {
      echo "$header"
      echo ""
      echo "총 $(wc -l < "$rows_file" | tr -d ' ')장 카드. GitHub 이슈 발행 상태는 각 카드 안의 Meta.\`GitHub Issue\` 필드로 추적."
      echo ""
      echo "| ID | 제목 | Size | Area | Domain |"
      echo "|---|---|---|---|---|"
      cat "$rows_file"
    } > "$WIKI_DIR/${page}.md"
    TRANSFORMED=$((TRANSFORMED + 1))
  done
fi

# --- Step 6: Generate Home, Sidebar, How-It-Works ---

echo "[6/6] Generating Home, Sidebar, How-It-Works..."

TOTAL_CARDS=$(find "$WIKI_DIR" -maxdepth 1 -name 'Task-T-*.md' | wc -l | tr -d ' ')
M1_COUNT=$([ -f "$ROWS_DIR/M1.rows" ] && wc -l < "$ROWS_DIR/M1.rows" | tr -d ' ' || echo 0)
M2_COUNT=$([ -f "$ROWS_DIR/M2.rows" ] && wc -l < "$ROWS_DIR/M2.rows" | tr -d ' ' || echo 0)
M3_COUNT=$([ -f "$ROWS_DIR/M3.rows" ] && wc -l < "$ROWS_DIR/M3.rows" | tr -d ' ' || echo 0)
M4_COUNT=$([ -f "$ROWS_DIR/M4.rows" ] && wc -l < "$ROWS_DIR/M4.rows" | tr -d ' ' || echo 0)
CROSS_COUNT=$([ -f "$ROWS_DIR/Cross-cutting.rows" ] && wc -l < "$ROWS_DIR/Cross-cutting.rows" | tr -d ' ' || echo 0)

cat > "$WIKI_DIR/Home.md" <<EOF
# Tourwave Roadmap

마지막 sync: $(date -u +'%Y-%m-%d %H:%M:%S UTC')

## 대시보드

| 마일스톤 | 카드 수 | 상태 |
|---|---|---|
| M1 (인증·탐색) | $M1_COUNT | 카드 준비됨 |
| M2 (예약·결제) | $M2_COUNT | 카드 미생성 |
| M3 (운영·CS) | $M3_COUNT | 카드 미생성 |
| M4 (리포팅·정책) | $M4_COUNT | 카드 미생성 |
| Cross-cutting (인프라) | $CROSS_COUNT | 카드 준비됨 |
| **총합** | **$TOTAL_CARDS** | |

## 탐색

- [감사 문서](Audit) — BE/FE 도메인별 현황 스냅샷
- [갭 매트릭스](Gap-Matrix) — SSOT ↔ 현 상태 비교
- [에이전트 워크플로우 가이드](How-It-Works)

### 마일스톤

- [M1 — 인증·탐색](Milestone-M1)
- [M2 — 예약·결제](Milestone-M2)
- [M3 — 운영·CS](Milestone-M3)
- [M4 — 리포팅·정책](Milestone-M4)
- [Cross-cutting](Milestone-Cross)

## Projects 칸반

실시간 진행 상태는 [GitHub Projects: Tourwave Roadmap](https://github.com/users/${OWNER}/projects) 확인.
EOF
TRANSFORMED=$((TRANSFORMED + 1))

cat > "$WIKI_DIR/_Sidebar.md" <<EOF
## 문서

- [[Home]]
- [[How-It-Works]]
- [[Audit]]
- [[Gap-Matrix]]

## 마일스톤

- [[Milestone-M1]]
- [[Milestone-M2]]
- [[Milestone-M3]]
- [[Milestone-M4]]
- [[Milestone-Cross]]
EOF
TRANSFORMED=$((TRANSFORMED + 1))

cat > "$WIKI_DIR/How-It-Works.md" <<EOF
# How It Works

이 위키는 \`docs/\` 디렉토리의 마크다운에서 자동 생성된다. 사람은 위키를 직접 편집하지 않고, 리포의 \`docs/\` 를 수정한 뒤 \`scripts/publish-wiki.sh\` 를 실행한다.

## 산출물

1. **감사 문서** (\`docs/audit/\`): 도메인별 현재 상태 체크리스트
2. **갭 매트릭스** (\`docs/gap-matrix.md\`): SSOT가 요구하는 것 대 현 구현
3. **태스크 카드** (\`docs/tasks/M*/\`, \`docs/tasks/cross/\`): 에이전트 실행 단위

## 태스크 카드 구조

각 카드는 다음 섹션 포함:

- **Meta**: ID, Milestone, Domain, Area, Size, Depends on, Blocks, GitHub Issue, Status
- **파일 소유권**: WRITE / READ / DO NOT TOUCH — 동시 디스패치 시 파일 충돌 방지
- **SSOT 근거**: openapi.yaml / policies.md 등 기존 문서의 섹션 참조
- **현재 상태 (갭)**: 지금 없는 것
- **구현 지침**: 순서대로 할 일
- **Acceptance Criteria**: 검증 가능한 완료 기준
- **Verification**: \`./scripts/verify-task.sh T-XXX\` 통과 요건
- **Rollback**: 문제 시 되돌리는 법

## 에이전트 워크플로우

1. Projects 칸반에서 Ready 컬럼의 카드 집기
2. \`./scripts/task-start.sh T-XXX\` — 워크트리 생성
3. 카드의 구현 지침에 따라 코드 수정 (WRITE 리스트 바깥 파일은 손대지 말 것)
4. \`./scripts/verify-task.sh T-XXX\` — 검증 (build/lint/test/security/docs)
5. \`./scripts/task-finish.sh T-XXX\` — PR 생성
6. 리뷰 통과 시 머지, Projects에서 Done 이동

## 동시 디스패치 규칙

한 세션에서 2~4개 카드를 병렬로 실행하려면:

- 각 카드의 WRITE 리스트 교집합 ∅ (validate-tasks.sh 통과)
- Depends on 대상이 모두 Done
- 서로 다른 worktree에서 실행
- 테스트 DB/포트 격리 (Testcontainers는 자동 할당)

## 재생성

카드/매트릭스가 바뀌면:

\`\`\`bash
./scripts/validate-tasks.sh      # 정합성 검증
./scripts/publish-wiki.sh        # 위키 sync
./scripts/publish-tasks.sh M1    # 이슈 발행 (최초 1회, 마일스톤별)
\`\`\`
EOF
TRANSFORMED=$((TRANSFORMED + 1))

# --- Commit & push ---

if [ "$DRY_RUN" -eq 1 ]; then
  echo ""
  echo "=== Dry-run Summary ==="
  echo "Would sync $TRANSFORMED pages. Preview:"
  ls -1 "$WIKI_DIR" | grep -v '^\.' | head -25
  total=$(ls -1 "$WIKI_DIR" | grep -v '^\.' | wc -l | tr -d ' ')
  if [ "$total" -gt 25 ]; then
    echo "... ($total total)"
  fi
  exit 0
fi

echo ""
echo "Committing & pushing..."
cd "$WIKI_DIR"
git add -A
if git diff --cached --quiet; then
  echo "Nothing changed. Skipping push."
  exit 0
fi
git commit -m "sync: $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
git push

echo ""
echo "=== Summary ==="
echo "Synced $TRANSFORMED pages to wiki"
