# T-025 — [FE] Tours — 투어 생성/수정 폼 (운영자)

## Meta
- ID: T-025
- Milestone: M1 (인증·탐색)
- Domain: Tours
- Area: FE
- Layer: ui
- Size: L (~5h)
- Depends on: T-010, T-911, T-912, T-916
- Blocks: T-026
- GitHub Issue: #41 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/app/(operator)/tours/new/page.tsx` (신규)
  - `src/app/(operator)/tours/[id]/edit/page.tsx` (신규)
  - `src/features/tours/TourForm.tsx` (신규)
  - `src/features/tours/TourContentEditor.tsx` (신규)

READ:
  - `docs/openapi.yaml` (Tours 태그, paths: `POST /tours`, `PATCH /tours/{tourId}`, `PUT /tours/{tourId}/content`, `GET /tours/{tourId}/instructors`)
  - `src/lib/api/schema.ts` (T-010에서 생성된 타입)
  - `src/lib/api/client.ts` (T-010에서 생성된 fetch 래퍼)

DO NOT TOUCH:
  - `src/features/tours/TourCard.tsx` (T-023의 파일)
  - `src/features/tours/TourDetail.tsx` (T-024의 파일)

## SSOT 근거
- `docs/openapi.yaml` paths: `post /tours` (create), `patch /tours/{tourId}` (update), `put /tours/{tourId}/content` (content), `put /tours/{tourId}/instructors` (instructors)
- `docs/audit/FE-tours.md` §관찰된 문제: "Route not implemented"

## 현재 상태 (갭)
- [ ] 투어 생성/수정 폼 페이지 없음
- [ ] 폼 검증 로직 없음
- [ ] API 호출 미구현

## 구현 지침
1. `src/app/(operator)/tours/new/page.tsx` 생성 (보호된 라우트, T-912 인증 필수)
2. `src/app/(operator)/tours/[id]/edit/page.tsx` 생성 (수정 페이지, 기존 투어 데이터 로드)
3. `src/features/tours/TourForm.tsx` 생성 (클라이언트 컴포넌트)
   - 기본 정보: 제목, 설명, 카테고리, 포맷, 위치, 가격 등
   - 강사 선택 (multi-select, `/tours/{tourId}/instructors` 호출)
   - 이미지 업로드 (T-911 Asset 활용)
4. `src/features/tours/TourContentEditor.tsx` 생성 (rich text editor, 일정/준비물/주의사항)
5. 폼 검증: 필드 길이, 가격 범위, 카테고리 유효성 (BE 스키마 맞춤)
6. 생성 시 `POST /tours` 호출 (Idempotency-Key 자동)
7. 수정 시 `PATCH /tours/{id}` + `PUT /tours/{id}/content` 호출
8. 강사 설정: `PUT /tours/{id}/instructors`
9. 성공 시 → 투어 상세 페이지로 리디렉션 (T-024)
10. 에러 처리: 409, 422 등 (사용자 피드백)

## Acceptance Criteria
- [ ] `src/app/(operator)/tours/new/page.tsx` 렌더링 완료
- [ ] `src/app/(operator)/tours/[id]/edit/page.tsx` 렌더링 완료
- [ ] 폼 필드 검증 (클라이언트 + 서버 에러 처리)
- [ ] `POST /tours` 호출 성공 시 투어 생성
- [ ] `PATCH /tours/{id}` 호출 성공 시 투어 수정
- [ ] 이미지 업로드 작동 (T-911 Asset)
- [ ] 강사 선택 작동 (multi-select)
- [ ] 리디렉션 작동 (생성/수정 후 상세 페이지로)
- [ ] 로딩 상태 UI 표시
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과

## Verification
`./scripts/verify-task.sh T-025`

## Rollback
```bash
git clean -fd src/app/\(operator\)/tours/
git clean -fd src/features/tours/TourForm.tsx
git clean -fd src/features/tours/TourContentEditor.tsx
```

## Notes
- 폼 라이브러리: react-hook-form 권장
- Rich text editor: slate, lexical, 또는 markdown editor 검토
- 이미지는 T-911 Asset 어댑터 활용 (업로드, 순서 변경)
- Idempotency-Key는 T-010 fetch 래퍼에서 자동 생성
- 강사 목록은 `/organizations/{orgId}/members`에서 조회 가능
