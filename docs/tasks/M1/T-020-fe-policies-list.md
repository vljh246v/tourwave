# T-020 — [FE] Policies — 정책 페이지 (읽기 전용)

## Meta
- ID: T-020
- Milestone: M1 (인증·탐색)
- Domain: Policies
- Area: FE
- Layer: ui
- Size: S (~2h)
- Depends on: T-010, T-911, T-916
- Blocks: T-021
- GitHub Issue: #36 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/app/(public)/policies/page.tsx` (신규)
  - `src/features/policies/PoliciesList.tsx` (신규)

READ:
  - `docs/openapi.yaml` (Policies 태그)
  - `src/lib/api/schema.ts` (T-010에서 생성된 타입)

DO NOT TOUCH:
  - `src/app/(public)/` 하의 다른 라우트

## SSOT 근거
- `docs/openapi.yaml` path: `get /occurrences/{occurrenceId}/policies` (공개)
- `docs/audit/FE-policies.md` §관찰된 문제: "Route not implemented"
- 정책은 관리자에 의해 관리되며 공개 페이지에서 읽기 전용으로 표시됨

## 현재 상태 (갭)
- [ ] 정책 목록 페이지 없음
- [ ] 정책 정보 표시 UI 없음

## 구현 지침
1. `src/app/(public)/policies/page.tsx` 생성 (서버 컴포넌트, 정적 또는 캐시)
2. `src/features/policies/PoliciesList.tsx` 생성 (클라이언트 컴포넌트)
3. 정책 종류: 취소 정책, 환불 정책, 이용 정책 등 (BE 스키마 확인)
4. 각 정책을 카드/섹션으로 표시 (읽기 전용, 마크다운 또는 HTML)
5. 정책별 상세 페이지 링크 (T-021으로)
6. 검색/필터 기능 (선택)

## Acceptance Criteria
- [ ] `src/app/(public)/policies/page.tsx` 렌더링 완료
- [ ] 정책 목록 표시 (최소 3개)
- [ ] 각 정책 카드에서 "상세 보기" 링크 작동 (T-021으로)
- [ ] 반응형 레이아웃
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과

## Verification
`./scripts/verify-task.sh T-020`

## Rollback
```bash
git clean -fd src/app/\(public\)/policies/
git clean -fd src/features/policies/PoliciesList.tsx
```

## Notes
- 정책 내용은 백엔드에서 관리되는 마스터 데이터 (API 또는 정적 config)
- 다국어 지원 필요 시 i18n 라이브러리 활용
