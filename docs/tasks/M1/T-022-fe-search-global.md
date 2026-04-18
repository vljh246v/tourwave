# T-022 — [FE] Search — 전역 검색 UI

## Meta
- ID: T-022
- Milestone: M1 (인증·탐색)
- Domain: Search
- Area: FE
- Layer: ui
- Size: M (~3h)
- Depends on: T-010, T-911, T-916
- Blocks: T-027
- GitHub Issue: #38 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/features/search/GlobalSearchBar.tsx` (신규)
  - `src/features/search/SearchInput.tsx` (신규)

READ:
  - `docs/openapi.yaml` (Search 태그, path: `GET /search/occurrences`)
  - `src/lib/api/schema.ts` (T-010에서 생성된 타입)
  - `src/lib/api/client.ts` (T-010에서 생성된 fetch 래퍼)

DO NOT TOUCH:
  - `src/app/layout.tsx` (글로벌 레이아웃 — 삽입은 후속 통합 태스크에서)

## SSOT 근거
- `docs/openapi.yaml` path: `get /search/occurrences` (공개, 검색)
- `docs/audit/FE-search.md` §관찰된 문제: "Route not implemented"

## 현재 상태 (갭)
- [ ] 전역 검색 바 없음
- [ ] 검색 입력 UI 없음
- [ ] 검색 필터 UI 없음

## 구현 지침
1. `src/features/search/GlobalSearchBar.tsx` 생성 (클라이언트 컴포넌트)
2. 검색 입력 필드 + 검색 버튼 구성
3. 필터 옵션: 날짜 범위, 가격 범위, 위치, 카테고리 등 (BE 스키마 확인)
4. 검색 입력 시 디바운싱 (300ms) 적용
5. 검색 결과 페이지 (T-027)로 라우팅
6. 검색 기록 저장 (localStorage, 선택)
7. 반응형: 모바일 → 축약 바, 데스크톱 → 확장 바

## Acceptance Criteria
- [ ] `src/features/search/GlobalSearchBar.tsx` 렌더링 완료
- [ ] 검색 입력 필드 작동
- [ ] 필터 옵션 표시 (최소 3개)
- [ ] 검색 버튼 클릭 시 `T-027` 검색 결과 페이지로 이동
- [ ] 디바운싱 작동 (불필요한 API 호출 방지)
- [ ] 반응형 레이아웃
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과

## Verification
`./scripts/verify-task.sh T-022`

## Rollback
```bash
git clean -fd src/features/search/GlobalSearchBar.tsx
git clean -fd src/features/search/SearchInput.tsx
```

## Notes
- 검색 바는 헤더/네비게이션 컴포넌트에 삽입 (T-022 자체는 컴포넌트만 생성)
- 레이아웃 삽입은 별도 통합 태스크 또는 T-027 검색 결과 페이지 구현 후 진행
- 자동완성 (autocomplete)은 phase 3+ 기능으로 미연기
