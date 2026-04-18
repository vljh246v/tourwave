# T-027 — [FE] Search — 검색 결과 페이지

## Meta
- ID: T-027
- Milestone: M1 (인증·탐색)
- Domain: Search
- Area: FE
- Layer: ui
- Size: M (~3h)
- Depends on: T-010, T-911, T-916, T-022
- Blocks: 없음
- GitHub Issue: #43 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/app/(public)/search/page.tsx` (신규)
  - `src/features/search/SearchResults.tsx` (신규)

READ:
  - `docs/openapi.yaml` (Search 태그, path: `GET /search/occurrences`)
  - `src/lib/api/schema.ts` (T-010에서 생성된 타입)
  - `src/lib/api/client.ts` (T-010에서 생성된 fetch 래퍼)

DO NOT TOUCH:
  - `src/features/search/GlobalSearchBar.tsx` (T-022의 파일)

## SSOT 근거
- `docs/openapi.yaml` path: `get /search/occurrences` (공개, 검색 결과)
- `docs/audit/FE-search.md` §관찰된 문제: "Route not implemented"

## 현재 상태 (갭)
- [ ] 검색 결과 페이지 없음
- [ ] API 호출 미구현
- [ ] 결과 렌더링 UI 없음

## 구현 지침
1. `src/app/(public)/search/page.tsx` 생성 (서버 컴포넌트)
2. 쿼리 파라미터 기반 검색: q (검색어), dateFrom, dateTo, timezone, locationText, categoryId, formatId, priceMin, priceMax, onlyAvailable, partySize, cursor, limit
3. `GET /search/occurrences` 호출 (쿼리 파라미터 포함)
4. `src/features/search/SearchResults.tsx` 생성 (클라이언트 컴포넌트)
5. 발생(occurrence) 결과 표시: 투어명, 날짜, 가격, 위치, 이미지, 별점 등
6. 결과 없음 상태 표시 ("검색 결과가 없습니다" 메시지)
7. 필터 재정렬/재적용 UI (T-022와 통일)
8. 페이지네이션 또는 무한 스크롤 (cursor 기반)
9. 결과 정렬: 인기도, 가격, 날짜 등

## Acceptance Criteria
- [ ] `src/app/(public)/search/page.tsx` 렌더링 완료
- [ ] `GET /search/occurrences` 호출 성공 시 결과 표시
- [ ] 발생 결과 카드 렌더링 (최소 5개)
- [ ] 결과 없음 상태 메시지 표시
- [ ] 필터 재적용 UI 작동
- [ ] 페이지네이션/무한 스크롤 작동
- [ ] 반응형 레이아웃
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과

## Verification
`./scripts/verify-task.sh T-027`

## Rollback
```bash
git clean -fd src/app/\(public\)/search/
git clean -fd src/features/search/SearchResults.tsx
```

## Notes
- 검색 결과 카드는 T-023 TourCard를 재사용 가능 (occurrence 기반으로 조정)
- cursor 기반 페이지네이션 (offset 불가능)
- 검색 기록 저장은 T-022에서 처리 (localStorage)
