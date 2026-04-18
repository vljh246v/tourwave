# T-017 — [FE] Organizations — 조직 상세 페이지

## Meta
- ID: T-017
- Milestone: M1 (인증·탐색)
- Domain: Organizations
- Area: FE
- Layer: ui
- Size: M (~3h)
- Depends on: T-010, T-911, T-916
- Blocks: T-019
- GitHub Issue: #33 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/app/(public)/organizations/[id]/page.tsx` (신규)
  - `src/features/organizations/OrganizationDetail.tsx` (신규)

READ:
  - `docs/openapi.yaml` (Organizations 태그, path: `GET /organizations/{orgId}`)
  - `src/lib/api/schema.ts` (T-010에서 생성된 타입)
  - `src/lib/api/client.ts` (T-010에서 생성된 fetch 래퍼)

DO NOT TOUCH:
  - `src/features/organizations/OrganizationCard.tsx` (T-016의 파일)

## SSOT 근거
- `docs/openapi.yaml` path: `get /organizations/{orgId}` (공개, security: [])
- `docs/audit/FE-organizations.md` §관찰된 문제: "Route not implemented"

## 현재 상태 (갭)
- [ ] 조직 상세 페이지 없음
- [ ] 동적 라우팅 ([id] 파라미터) 미구현
- [ ] 조직 정보 조회 API 호출 없음

## 구현 지침
1. `src/app/(public)/organizations/[id]/page.tsx` 생성 (서버 컴포넌트, 동적 라우팅)
2. 라우트 파라미터에서 `id` 추출 후 `GET /organizations/{id}` 호출
3. `src/features/organizations/OrganizationDetail.tsx` 생성 (클라이언트 컴포넌트)
4. 조직 정보 표시: 이름, 설명, 로고, 멤버 수, 투어 수 등
5. 조직의 투어 목록 링크 제공 (T-023과 연계)
6. 운영자인 경우 편집/멤버 관리 버튼 표시 (조건부 렌더링, T-912 인증 확인)
7. 404 처리 (조직 없음 → 404 페이지)

## Acceptance Criteria
- [ ] `src/app/(public)/organizations/[id]/page.tsx` 렌더링 완료
- [ ] `GET /organizations/{id}` 호출 성공 시 정보 표시
- [ ] 운영자 권한 확인 후 "편집" 버튼 표시 (조건부)
- [ ] 존재하지 않는 조직 → 404 페이지 표시
- [ ] 반응형 레이아웃
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과

## Verification
`./scripts/verify-task.sh T-017`

## Rollback
```bash
git clean -fd src/app/\(public\)/organizations/\[id\]/
git clean -fd src/features/organizations/OrganizationDetail.tsx
```

## Notes
- 라우트 파라미터 검증: 숫자/문자열 여부 확인
- 조직의 로고/배경 이미지는 Asset 정보 활용 (T-911과 협업)
