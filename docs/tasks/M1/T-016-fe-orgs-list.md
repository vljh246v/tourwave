# T-016 — [FE] Organizations — 조직 목록 (공개)

## Meta
- ID: T-016
- Milestone: M1 (인증·탐색)
- Domain: Organizations
- Area: FE
- Layer: ui
- Size: M (~3h)
- Depends on: T-010, T-911, T-916
- Blocks: T-017, T-018
- GitHub Issue: #32 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/app/(public)/organizations/page.tsx` (신규)
  - `src/features/organizations/OrganizationList.tsx` (신규)
  - `src/features/organizations/OrganizationCard.tsx` (신규)

READ:
  - `docs/openapi.yaml` (Organizations 태그)
  - `src/lib/api/schema.ts` (T-010에서 생성된 타입)
  - `src/lib/api/client.ts` (T-010에서 생성된 fetch 래퍼)

DO NOT TOUCH:
  - `src/app/layout.tsx` (글로벌 레이아웃 — T-022 통합 담당)
  - `src/features/` 하의 다른 도메인 디렉토리

## SSOT 근거
- `docs/openapi.yaml` path: `get /organizations/{orgId}` (공개) + `get /organizations` (없음, 목록 구현 필요)
- `docs/audit/FE-organizations.md` §관찰된 문제: "Route not implemented"

## 현재 상태 (갭)
- [ ] 공개 조직 목록 페이지 없음
- [ ] 조직 카드 컴포넌트 없음
- [ ] API 클라이언트 호출 미구현

## 구현 지침
1. `src/app/(public)/organizations/page.tsx` 생성 (서버 컴포넌트)
2. 페이지에서 `GET /organizations` 호출 (T-010의 fetch 래퍼 사용)
3. `src/features/organizations/OrganizationList.tsx` 생성 (클라이언트 컴포넌트, 목록 렌더링)
4. `src/features/organizations/OrganizationCard.tsx` 생성 (조직 카드: 로고, 이름, 설명, 링크)
5. 조직명/설명으로 검색 필터 추가 (선택)
6. 페이지네이션 또는 무한 스크롤 구현
7. 에러 상태 표시 (404, 5xx → 사용자 친화 메시지)

## Acceptance Criteria
- [ ] `src/app/(public)/organizations/page.tsx` 렌더링 완료
- [ ] 조직 카드 목록 표시 (최소 5개 항목)
- [ ] 클릭 시 `T-017` 상세 페이지로 이동
- [ ] 반응형 레이아웃 (모바일/태블릿/데스크톱)
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과

## Verification
`./scripts/verify-task.sh T-016`

## Rollback
```bash
git clean -fd src/app/\(public\)/organizations/
git clean -fd src/features/organizations/
```

## Notes
- API endpoint `/organizations` 목록이 OpenAPI에 정의되지 않음 — BE와 협의하여 add endpoint 또는 `/organizations/{orgId}` 단일 조회 후 클라이언트 캐싱
- 조직 로고는 Asset 정보에서 참조 가능 (향후 T-911과 통합)
