# T-021 — [FE] Policies — 정책 상세 페이지

## Meta
- ID: T-021
- Milestone: M1 (인증·탐색)
- Domain: Policies
- Area: FE
- Layer: ui
- Size: S (~2h)
- Depends on: T-010, T-911, T-916, T-020
- Blocks: 없음
- GitHub Issue: #37 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/app/(public)/policies/[slug]/page.tsx` (신규)
  - `src/features/policies/PolicyDetail.tsx` (신규)

READ:
  - `docs/openapi.yaml` (Policies 태그)
  - `src/lib/api/schema.ts` (T-010에서 생성된 타입)
  - `src/lib/api/client.ts` (T-010에서 생성된 fetch 래퍼)

DO NOT TOUCH:
  - `src/features/policies/PoliciesList.tsx` (T-020의 파일)

## SSOT 근거
- `docs/openapi.yaml` path: `get /occurrences/{occurrenceId}/policies` (공개, 정책 조회)
- `docs/audit/FE-policies.md` §관찰된 문제: "Route not implemented"

## 현재 상태 (갭)
- [ ] 정책 상세 페이지 없음
- [ ] 동적 라우팅 ([slug] 파라미터) 미구현
- [ ] 정책 전문 표시 UI 없음

## 구현 지침
1. `src/app/(public)/policies/[slug]/page.tsx` 생성 (서버 컴포넌트, 동적 라우팅)
2. 라우트 파라미터에서 `slug` 추출 (예: "cancellation", "refund")
3. 정책 정보 조회 (API 호출 또는 정적 config 기반)
4. `src/features/policies/PolicyDetail.tsx` 생성 (클라이언트 컴포넌트)
5. 정책 전문 표시: 제목, 내용 (마크다운 또는 HTML), 최종 수정일
6. 404 처리 (정책 없음)
7. 뒤로 가기 버튼 (T-020 정책 목록으로)

## Acceptance Criteria
- [ ] `src/app/(public)/policies/[slug]/page.tsx` 렌더링 완료
- [ ] 정책 전문 표시 완료
- [ ] 존재하지 않는 정책 slug → 404 페이지 표시
- [ ] 마크다운/HTML 렌더링 (필요 시 라이브러리 사용)
- [ ] 반응형 레이아웃
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과

## Verification
`./scripts/verify-task.sh T-021`

## Rollback
```bash
git clean -fd src/app/\(public\)/policies/\[slug\]/
git clean -fd src/features/policies/PolicyDetail.tsx
```

## Notes
- slug 유효성 검증 필요 (특수문자 제외)
- 정책 버전 관리 고려 (향후 feature)
