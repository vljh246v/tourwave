# T-024 — [FE] Tours — 투어 상세 페이지

## Meta
- ID: T-024
- Milestone: M1 (인증·탐색)
- Domain: Tours
- Area: FE
- Layer: ui
- Size: L (~4h)
- Depends on: T-010, T-911, T-916
- Blocks: T-025, T-026
- GitHub Issue: #40 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/app/(public)/tours/[id]/page.tsx` (신규)
  - `src/features/tours/TourDetail.tsx` (신규)
  - `src/features/tours/OccurrenceList.tsx` (신규)

READ:
  - `docs/openapi.yaml` (Tours 태그, paths: `GET /tours/{tourId}`, `GET /tours/{tourId}/instructors`, `GET /tours/{tourId}/content`)
  - `src/lib/api/schema.ts` (T-010에서 생성된 타입)
  - `src/lib/api/client.ts` (T-010에서 생성된 fetch 래퍼)

DO NOT TOUCH:
  - `src/features/tours/TourCard.tsx` (T-023의 파일)

## SSOT 근거
- `docs/openapi.yaml` paths: `get /tours/{tourId}` (public if published), `get /tours/{tourId}/instructors`, `get /tours/{tourId}/content`
- `docs/audit/FE-tours.md` §관찰된 문제: "Route not implemented"

## 현재 상태 (갭)
- [ ] 투어 상세 페이지 없음
- [ ] 투어 정보/강사/콘텐츠 조회 API 호출 없음
- [ ] 발생 목록 표시 없음

## 구현 지침
1. `src/app/(public)/tours/[id]/page.tsx` 생성 (서버 컴포넌트, 동적 라우팅)
2. `GET /tours/{id}` + `GET /tours/{id}/instructors` + `GET /tours/{id}/content` 병렬 호출
3. `src/features/tours/TourDetail.tsx` 생성 (클라이언트 컴포넌트)
4. 투어 정보 표시: 제목, 설명, 대표 이미지, 가격, 카테고리, 위치, 난이도 등
5. 강사 정보 표시: 이름, 프로필 사진, 소개 (T-911과 협업)
6. 콘텐츠(rich text) 표시: 일정, 준비물, 주의사항 등
7. `src/features/tours/OccurrenceList.tsx` 생성 (발생 목록: 날짜, 정원, 가격, "예약" 버튼)
8. 운영자인 경우 "편집" 버튼 표시 (T-025로 이동, 조건부 렌더링 + T-912 인증)
9. 404 처리 (투어 없음 또는 draft 상태)

## Acceptance Criteria
- [ ] `src/app/(public)/tours/[id]/page.tsx` 렌더링 완료
- [ ] 투어 정보 표시 (제목, 설명, 이미지, 가격 등)
- [ ] 강사 정보 표시
- [ ] 발생 목록 표시 (최소 3개)
- [ ] 운영자 권한 확인 후 "편집" 버튼 표시 (조건부)
- [ ] draft 상태 투어 → 403 또는 404
- [ ] 반응형 레이아웃
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과

## Verification
`./scripts/verify-task.sh T-024`

## Rollback
```bash
git clean -fd src/app/\(public\)/tours/\[id\]/
git clean -fd src/features/tours/TourDetail.tsx
git clean -fd src/features/tours/OccurrenceList.tsx
```

## Notes
- 이미지/강사 정보는 Asset 및 Instructor 도메인과 협업 (T-911)
- 콘텐츠는 마크다운 또는 HTML (라이브러리 필요 시 확인)
- 발생 목록 클릭 → T-026 또는 예약 흐름 (phase 2)
