# T-018 — [FE] Organizations — 조직 생성 폼 (운영자)

## Meta
- ID: T-018
- Milestone: M1 (인증·탐색)
- Domain: Organizations
- Area: FE
- Layer: ui
- Size: M (~3h)
- Depends on: T-010, T-911, T-912, T-916
- Blocks: T-019, T-025
- GitHub Issue: #34 (생성 전)
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/app/(operator)/organizations/new/page.tsx` (신규)
  - `src/features/organizations/CreateForm.tsx` (신규)

READ:
  - `docs/openapi.yaml` (Organizations 태그, path: `POST /organizations`)
  - `src/lib/api/schema.ts` (T-010에서 생성된 타입)
  - `src/lib/api/client.ts` (T-010에서 생성된 fetch 래퍼)

DO NOT TOUCH:
  - `src/app/(operator)/` 하의 다른 라우트

## SSOT 근거
- `docs/openapi.yaml` path: `post /organizations` (creator becomes ORG_OWNER)
- `docs/audit/FE-organizations.md` §관찰된 문제: "Route not implemented"
- `docs/policies.md` (백엔드 repo): 조직 생성 권한 = 인증 사용자

## 현재 상태 (갭)
- [ ] 조직 생성 폼 페이지 없음
- [ ] 폼 검증 로직 없음
- [ ] API 호출 미구현

## 구현 지침
1. `src/app/(operator)/organizations/new/page.tsx` 생성 (보호된 라우트, T-912 인증 필수)
2. `src/features/organizations/CreateForm.tsx` 생성 (클라이언트 컴포넌트)
3. 폼 필드: 조직명 (required), 설명 (optional), 로고 이미지 (선택)
4. 폼 검증: 조직명 길이 제약, 설명 길이 제약 (BE 스키마에 맞춤)
5. `POST /organizations` 호출 (Idempotency-Key 헤더 자동 부착 by T-010)
6. 생성 성공 시 → `T-017` 상세 페이지로 리디렉션
7. 에러 처리: 409 (Conflict - 중복명?), 422 (Unprocessable) → 사용자 피드백
8. 로딩 상태 표시, 중복 제출 방지

## Acceptance Criteria
- [ ] `src/app/(operator)/organizations/new/page.tsx` 렌더링 완료
- [ ] 폼 필드 검증 (클라이언트 + 서버 에러 처리)
- [ ] `POST /organizations` 호출 성공 시 조직 생성
- [ ] 리디렉션 작동 (생성 후 상세 페이지로)
- [ ] 로딩 상태 UI 표시
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과

## Verification
`./scripts/verify-task.sh T-018`

## Rollback
```bash
git clean -fd src/app/\(operator\)/organizations/
git clean -fd src/features/organizations/CreateForm.tsx
```

## Notes
- 폼 라이브러리: react-hook-form 권장 (이미 설치 가정)
- 이미지 업로드는 T-911에서 Asset 관리 활용
- Idempotency-Key는 T-010 fetch 래퍼에서 자동 생성
