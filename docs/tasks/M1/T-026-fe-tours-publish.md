# T-026 — [FE] Tours — 투어 발행 액션 (운영자)

## Meta
- ID: T-026
- Milestone: M1 (인증·탐색)
- Domain: Tours
- Area: FE
- Layer: ui
- Size: M (~2h)
- Depends on: T-010, T-911, T-912, T-916, T-025
- Blocks: 없음
- GitHub Issue: #42 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/features/tours/PublishButton.tsx` (신규)
  - `src/features/tours/ArchiveButton.tsx` (신규)

READ:
  - `docs/openapi.yaml` (Tours 태그, paths: `POST /tours/{tourId}/publish`, `POST /tours/{tourId}/archive`)
  - `src/lib/api/schema.ts` (T-010에서 생성된 타입)
  - `src/lib/api/client.ts` (T-010에서 생성된 fetch 래퍼)

DO NOT TOUCH:
  - `src/app/(operator)/tours/[id]/edit/page.tsx` (T-025의 파일)
  - `src/app/(public)/tours/[id]/page.tsx` (T-024의 파일)

## SSOT 근거
- `docs/openapi.yaml` paths: `post /tours/{tourId}/publish` (DRAFT -> PUBLISHED), `post /tours/{tourId}/archive` (PUBLISHED -> ARCHIVED)
- `docs/audit/FE-tours.md` §관찰된 문제: "Route not implemented"
- `docs/domain-rules.md` (백엔드 repo): 투어 상태 머신 (DRAFT → PUBLISHED → ARCHIVED)

## 현재 상태 (갭)
- [ ] 투어 발행 버튼 없음
- [ ] 투어 보관 버튼 없음
- [ ] API 호출 미구현

## 구현 지침
1. `src/features/tours/PublishButton.tsx` 생성 (클라이언트 컴포넌트)
   - 버튼: 상태에 따라 "발행" 또는 "발행됨" 표시
   - DRAFT 상태일 때만 활성화
   - `POST /tours/{tourId}/publish` 호출 (Idempotency-Key 자동)
   - 성공 시 상태 업데이트 (또는 페이지 새로고침)
2. `src/features/tours/ArchiveButton.tsx` 생성 (클라이언트 컴포넌트)
   - 버튼: "보관"
   - PUBLISHED 상태일 때만 활성화
   - `POST /tours/{tourId}/archive` 호출 (Idempotency-Key 자동)
   - 확인 모달: "보관하시겠습니까?" (되돌릴 수 없음)
3. 에러 처리: 409 (conflict - 이미 발행됨), 403 (권한 없음) 등
4. 로딩 상태 표시 (버튼 disabled, 로딩 스피너)
5. 성공 토스트/스낵바 표시

## Acceptance Criteria
- [ ] `src/features/tours/PublishButton.tsx` 렌더링 완료
- [ ] `src/features/tours/ArchiveButton.tsx` 렌더링 완료
- [ ] PublishButton: DRAFT 상태 시 활성화, 발행 호출 성공
- [ ] ArchiveButton: PUBLISHED 상태 시 활성화, 보관 호출 성공
- [ ] 확인 모달 작동 (ArchiveButton)
- [ ] 에러 처리 (409, 403 등)
- [ ] 로딩 상태 UI
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과

## Verification
`./scripts/verify-task.sh T-026`

## Rollback
```bash
git clean -fd src/features/tours/PublishButton.tsx
git clean -fd src/features/tours/ArchiveButton.tsx
```

## Notes
- 버튼은 T-024/T-025 상세/편집 페이지에 주입 (각 페이지에서 import 사용)
- 이 태스크는 컴포넌트 생성만 담당 (페이지 파일 수정 없음)
- Idempotency-Key는 T-010 fetch 래퍼에서 자동 생성
- 상태 업데이트는 revalidate path 또는 SWR/React Query 사용 (검토 필요)
