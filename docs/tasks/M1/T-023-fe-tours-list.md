# T-023 — [FE] Tours — 투어 목록 (공개)

## Meta
- ID: T-023
- Milestone: M1 (인증·탐색)
- Domain: Tours
- Area: FE
- Layer: ui
- Size: L (~4h)
- Depends on: T-010, T-911, T-916
- Blocks: T-024, T-025
- GitHub Issue: #39 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/app/(public)/tours/page.tsx` (신규)
  - `src/features/tours/TourList.tsx` (신규)
  - `src/features/tours/TourCard.tsx` (신규)
  - `src/features/tours/TourFilters.tsx` (신규)

READ:
  - `docs/openapi.yaml` (Tours 태그, path: `GET /tours`)
  - `src/lib/api/schema.ts` (T-010에서 생성된 타입)
  - `src/lib/api/client.ts` (T-010에서 생성된 fetch 래퍼)

DO NOT TOUCH:
  - `src/features/search/GlobalSearchBar.tsx` (T-022의 파일)

## SSOT 근거
- `docs/openapi.yaml` path: `get /tours` (공개, published only, pagination)
- `docs/audit/FE-tours.md` §관찰된 문제: "Route not implemented"

## 현재 상태 (갭)
- [ ] 공개 투어 목록 페이지 없음
- [ ] 투어 카드 컴포넌트 없음
- [ ] 필터/검색 UI 없음

## 구현 지침
1. `src/app/(public)/tours/page.tsx` 생성 (서버 컴포넌트)
2. 쿼리 파라미터 기반 필터: categoryId, formatId, locationText, dateFrom, dateTo, priceMin, priceMax 등
3. `GET /tours` 호출 (필터 파라미터 포함, T-010 fetch 래퍼 사용)
4. `src/features/tours/TourList.tsx` 생성 (클라이언트 컴포넌트, 투어 목록 렌더링)
5. `src/features/tours/TourCard.tsx` 생성 (투어 카드: 이미지, 제목, 설명, 가격, 별점 등)
6. `src/features/tours/TourFilters.tsx` 생성 (필터 사이드바 또는 상단)
7. 페이지네이션 또는 무한 스크롤 (cursor 기반)
8. 정렬 옵션: 인기도, 가격, 최신순
9. 로딩/에러 상태 표시

## Acceptance Criteria
- [ ] `src/app/(public)/tours/page.tsx` 렌더링 완료
- [ ] `GET /tours` 호출 성공 시 투어 목록 표시 (최소 10개)
- [ ] 필터 옵션 작동 (가격, 날짜, 위치 등)
- [ ] 투어 카드 클릭 시 `T-024` 상세 페이지로 이동
- [ ] 페이지네이션/무한 스크롤 작동
- [ ] 반응형 레이아웃 (그리드 1/2/3/4 컬럼)
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과

## Verification
`./scripts/verify-task.sh T-023`

## Rollback
```bash
git clean -fd src/app/\(public\)/tours/
git clean -fd src/features/tours/TourList.tsx
git clean -fd src/features/tours/TourCard.tsx
git clean -fd src/features/tours/TourFilters.tsx
```

## Notes
- 이미지는 Asset 정보에서 참조 (T-911)
- 별점/리뷰는 phase 2 기능 (T-023에서는 placeholder)
- cursor 기반 페이지네이션 (offset 불가능)
