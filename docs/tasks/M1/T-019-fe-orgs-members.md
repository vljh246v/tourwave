# T-019 — [FE] Organizations — 멤버 관리 UI (운영자)

## Meta
- ID: T-019
- Milestone: M1 (인증·탐색)
- Domain: Organizations
- Area: FE
- Layer: ui
- Size: M (~3h)
- Depends on: T-010, T-911, T-912, T-916, T-017, T-018
- Blocks: 없음
- GitHub Issue: #35 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/app/(operator)/organizations/[id]/members/page.tsx` (신규)
  - `src/features/organizations/MembersPanel.tsx` (신규)
  - `src/features/organizations/MemberRow.tsx` (신규)

READ:
  - `docs/openapi.yaml` (Organizations 태그, paths: `GET /organizations/{orgId}/members`, `POST /organizations/{orgId}/members`, `PATCH /organizations/{orgId}/members/{userId}`)
  - `src/lib/api/schema.ts` (T-010에서 생성된 타입)
  - `src/lib/api/client.ts` (T-010에서 생성된 fetch 래퍼)

DO NOT TOUCH:
  - `src/app/(public)/organizations/[id]/page.tsx` (T-017의 파일)

## SSOT 근거
- `docs/openapi.yaml` paths: `get /organizations/{orgId}/members`, `post /organizations/{orgId}/members`, `patch /organizations/{orgId}/members/{userId}`
- `docs/audit/FE-organizations.md` §관찰된 문제: "Route not implemented"

## 현재 상태 (갭)
- [ ] 멤버 관리 페이지 없음
- [ ] 멤버 목록 조회 API 호출 없음
- [ ] 멤버 추가/역할 변경 UI 없음

## 구현 지침
1. `src/app/(operator)/organizations/[id]/members/page.tsx` 생성 (보호된 라우트, ORG_OWNER/ORG_ADMIN만)
2. `GET /organizations/{orgId}/members` 호출 → 멤버 목록 조회
3. `src/features/organizations/MembersPanel.tsx` 생성 (멤버 테이블 렌더링)
4. `src/features/organizations/MemberRow.tsx` 생성 (개별 멤버 행: 이름, 이메일, 역할, 액션)
5. "멤버 추가" 버튼 → 모달 또는 폼 (이메일 입력, 역할 선택)
6. `POST /organizations/{orgId}/members` 호출 (Idempotency-Key 자동)
7. 역할 변경: `PATCH /organizations/{orgId}/members/{userId}` (드롭다운 선택 후)
8. 멤버 제거: (DELETE endpoint 있으면) 구현 또는 역할 제거
9. 권한 확인: ORG_OWNER만 역할 변경/제거 가능

## Acceptance Criteria
- [ ] `src/app/(operator)/organizations/[id]/members/page.tsx` 렌더링 완료
- [ ] `GET /organizations/{orgId}/members` 호출 성공 시 테이블 표시
- [ ] "멤버 추가" 버튼 작동 → `POST /organizations/{orgId}/members`
- [ ] 역할 변경 드롭다운 작동 → `PATCH /organizations/{orgId}/members/{userId}`
- [ ] 권한 없는 사용자 → 403 에러 처리
- [ ] 로딩/에러 상태 UI
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과

## Verification
`./scripts/verify-task.sh T-019`

## Rollback
```bash
git clean -fd src/app/\(operator\)/organizations/\[id\]/members/
git clean -fd src/features/organizations/MembersPanel.tsx
git clean -fd src/features/organizations/MemberRow.tsx
```

## Notes
- 멤버 역할: OWNER, ADMIN, OPERATOR 등 (백엔드 enum 참조)
- 이메일 검증은 BE에서 담당 (FE는 기본 형식만 확인)
- Idempotency-Key는 T-010 fetch 래퍼에서 자동 생성
