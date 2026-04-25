---
name: blockers
description: tourwave 프로젝트의 태스크 카드 블록커 관계를 조회한다. 다음에 어떤 카드를 작업할지 결정하거나, 특정 카드가 뭘 막는지/준비됐는지 확인하고 싶을 때 사용. 트리거 예시 — "블록커 보여줘", "다음 뭐 할까", "T-010이 막고 있는거", "지금 시작 가능한 카드", "ready 태스크".
---

# /blockers — 태스크 블록커 조회 및 다음 작업 추천

tourwave의 GitHub Projects 카드(`docs/tasks/**/T-*.md`) 간 `Depends on` / `Blocks` 관계를 분석해서 지금 시작 가능한 태스크를 찾아낸다.

## 언제 사용하나

- "뭐부터 할까" / "다음 태스크 추천" → 인자 없이 `/blockers`
- "T-XXX가 뭘 막고 있나" → `/blockers T-XXX`
- "지금 시작해도 되는 카드 뭐 있나" → `/blockers ready`
- "특정 마일스톤 블록커" → `/blockers M1` (또는 `cross`)

사용자 입력에 위 의도가 보이면 즉시 이 스킬 호출.

## 전제

- 작업 루트: `/Users/jaehyun/Documents/workspace/tourwave`
- 카드 위치: `docs/tasks/M*/T-*.md`, `docs/tasks/cross/T-*.md`
- 각 카드 `Meta` 섹션에 `- ID: T-XXX`, `- Depends on: ...`, `- Blocks: ...` 있음
- 각 카드에 `- GitHub Issue: #N` 필드 있음 (발행된 경우)
- 헬퍼 스크립트: `scripts/blocker-rank.sh` — 영향력 순위 + 개별 조회

## Step 1 — 입력 분석

사용자 입력을 한 단어로 파싱:

1. **`T-\d+` 패턴** (예: `T-010`) → 개별 조회 모드
2. **`ready`** (또는 "시작 가능", "지금 할 수 있는") → 준비된 카드 목록 모드
3. **`M1` / `M2` / `M3` / `M4` / `cross`** → 마일스톤 필터 + 순위 모드
4. **인자 없음** → 전역 순위 + 추천 모드

## Step 2 — 조회 실행

### 모드 A: 전역 순위 + 추천 (기본)

```bash
./scripts/blocker-rank.sh --top 10
```

출력 후 추가로:

- **상위 블록커 1~3개**에 대해 각각 카드 파일을 읽어 `Depends on`과 `Status` 필드 확인.
- **`Status: Done`인 카드는 건너뜀** (이미 완료된 카드는 블록커가 아님).
- Depends on이 **비어있거나 "없음"**이고 Status가 Done이 아닌 블록커 = **지금 바로 시작 가능한 최우선 카드**.
- 그 카드를 "추천 시작"으로 제시, 파일 경로 + `/dev` 명령 예시 함께.

예:
```
지금 당장 시작 가능한 최우선 블록커:
  T-912 — [FE] 인증 모듈 (11개 블로킹, Depends on: 없음)

추천 명령:
  /dev T-912 인증 모듈. 카드: docs/tasks/cross/T-912-fe-auth-module.md
```

### 모드 B: 개별 카드 조회 (`T-XXX`)

1. 카드 파일 위치 찾기: `find docs/tasks -name 'T-XXX-*.md'`
2. 카드의 `Depends on`, `Blocks`, `Status`, `GitHub Issue` 필드 추출
3. `./scripts/blocker-rank.sh T-XXX` 로 이 카드가 블록하는 목록
4. **이 카드가 지금 시작 가능한지** 판정:
   - `Status: Done`이면 → ✅ 이미 완료 (시작 불필요)
   - Depends on이 비어있거나 "없음"이면 → ✅ 즉시 가능
   - Depends on 목록의 각 카드에 대해 카드 파일의 `Status` 필드 확인 → `Done`이면 ✅, 아니면 ❌
   - GitHub 이슈 번호가 있으면 추가로 `gh issue view` 시도 (폴백: 카드 Status 우선)
   - 미완료 선행 카드가 남아있으면 → ❌ 대기 (어떤 카드가 남았는지 나열)

출력 예:
```
T-010 [FE] API Client
  상태: Backlog (#18)
  블로킹: 19개 카드
  필요 선행: T-911 (OpenAPI 타입), T-912 (인증 모듈)
  
  T-911 상태: Backlog — 미완료 ❌
  T-912 상태: Backlog — 미완료 ❌
  
  지금 시작 불가. 선행 2개 먼저:
    /blockers T-911
    /blockers T-912
```

### 모드 C: Ready 목록

`Depends on`이 "없음"이거나 비어있고 **`Status`가 Done이 아닌** 모든 카드 나열:

```bash
find docs/tasks -type f -name 'T-*.md' | while read f; do
  deps=$(grep -m1 '^- Depends on:' "$f" | sed 's/^- Depends on: *//')
  status=$(grep -m1 '^- Status:' "$f" | sed 's/^- Status: *//')
  # Done 카드는 건너뜀
  [ "$status" = "Done" ] && continue
  case "$deps" in
    ""|"없음")
      tid=$(basename "$f" | sed -E 's/^(T-[0-9]+).*/\1/')
      title=$(head -1 "$f" | sed 's/^# *//')
      block_count=$(find docs/tasks -type f -name 'T-*.md' -exec grep -lE "^- Depends on:.*$tid" {} \; 2>/dev/null | wc -l | tr -d ' ')
      printf "%3d  [%s] %s\n" "$block_count" "$status" "$title"
      ;;
  esac
done | sort -rn
```

블로킹 수 내림차순으로 정렬 → 상위부터 추천.

### 모드 D: 마일스톤 필터

`docs/tasks/M1/` 또는 `docs/tasks/cross/` 안만 스캔. 로직은 모드 A와 동일.

## Step 3 — 출력 포맷

- 한국어 요약
- 테이블 형식 (순위 | ID | 블록 수 | 제목)
- 추천 카드에 💡 이모지
- `/dev` 명령을 복붙 가능한 코드블록으로 제시
- 필요 시 관련 Project 링크: https://github.com/users/vljh246v/projects/4

## 주의사항

- `blocker-rank.sh`가 없으면 "scripts/blocker-rank.sh 없음 — Phase 4 퍼블리싱 완료 후 사용 가능" 안내
- `docs/tasks/` 가 비어있으면 "태스크 카드 없음 — Phase 3 카드 생성 먼저" 안내
- GitHub API 호출 실패 시 카드 파일만으로 폴백
- 카드 ID가 존재하지 않으면 친절한 에러

## 완료 후 제안

조회 결과를 보여준 뒤:
- 추천 카드 1개를 `/dev`로 시작할지 묻기
- 또는 여러 준비된 카드 중 병렬로 돌릴 2~3개 제안 (WRITE 충돌 없는 조합)
