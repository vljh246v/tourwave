# T-013 — [FE] Me — 사용자 프로필 페이지

## Meta
- ID: T-013
- Milestone: M1 (인증·탐색)
- Domain: Me
- Area: FE
- Layer: ui
- Size: M
- Depends on: T-010 (API 클라이언트), T-014 (프로필 수정 폼), T-912 (인증)
- Blocks: 없음
- GitHub Issue: #29
- Status: Backlog

## 파일 소유권 (FE 프로젝트 기준 경로)

WRITE:
  - `src/app/(me)/profile/page.tsx` — 프로필 페이지
  - `src/features/me/ProfileView.tsx` — 프로필 표시 컴포넌트 (읽기전용)

READ:
  - `docs/openapi.yaml` (Me tag)
  - `docs/policies.md` (인증 정책)
  - `src/lib/api/schema.ts` (T-911 생성 후)
  - `src/lib/auth/` (T-912 생성 후)
  - `src/features/me/ProfileEditForm.tsx` (T-014에서 생성)

DO NOT TOUCH:
  - T-014 ProfileEditForm (독립적으로 구현)
  - T-015 DeactivateModal (독립적으로 구현)
  - 다른 도메인의 컴포넌트

## SSOT 근거
- `docs/openapi.yaml` → paths:
  - `GET /me` (인증필수)
  - Response: `MeResponse` (id, email, displayName, emailVerified, createdAt, updatedAt 등)
  - Errors: 401 Unauthorized

## 현재 상태 (갭)
- [ ] 프로필 페이지 미구현
- [ ] 사용자 정보 조회 UI 없음
- [ ] 프로필 편집 폼 미연동

## 구현 지침

### 1. `ProfileView.tsx` — 읽기전용 프로필 표시
- 목적: 현재 사용자 정보 표시
- 표시 항목:
  - 프로필 이미지(선택) / 아바타 플레이스홀더
  - 표시명 (displayName)
  - 이메일 (email)
  - 이메일 검증 상태 (emailVerified: ✓/✗ 배지)
  - 계정 생성일 (createdAt, 상대 시간 또는 절대 시간)
  - 마지막 업데이트 (updatedAt)
  - 계정 상태 (활성/비활성)
- UI 패턴: 정보 카드 또는 섹션 레이아웃
- 로딩 상태: 스켈레톤 로더 또는 로딩 스피너

### 2. `/profile/page.tsx` — 페이지 컴포넌트
- 목적: 프로필 페이지 진입점
- 구성:
  - `ProfileView` — 상단 읽기전용 정보
  - "프로필 편집" 버튼 → T-014 `ProfileEditForm` 모달/드로어로 열기
  - "계정 설정" 섹션:
    - "비밀번호 변경" 버튼 (선택, T-012로 리다이렉트)
    - "계정 비활성화" 버튼 → T-015 `DeactivateModal` 열기
    - "계정 삭제" 버튼 (선택, T-015와 통합)
- 접근 제어: 인증필수 → 미인증 시 로그인 페이지로 리다이렉트 (T-912 미들웨어)
- 데이터 로딩: 페이지 진입 시 `GET /me` 호출 → 사용자 정보 조회

### 3. 데이터 흐름
```
페이지 로드
  ↓
GET /me 호출 (T-010 클라이언트)
  ↓
ProfileView 표시 (읽기전용)
  ↓
"편집" 버튼 클릭 → T-014 ProfileEditForm 표시 (모달)
  ↓
T-014에서 PATCH /me 성공 → ProfileView 새로고침
```

## Acceptance Criteria
- [ ] 페이지 진입 시 `GET /me` 호출 확인
- [ ] 사용자 정보 정상 표시 (displayName, email, emailVerified 상태)
- [ ] 로딩 상태 UI 표시 (스켈레톤 또는 스피너)
- [ ] 401 에러 시 로그인 페이지로 리다이렉트
- [ ] "프로필 편집" 버튼 클릭 → T-014 폼 표시
- [ ] "계정 비활성화" 버튼 클릭 → T-015 모달 표시
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과
- [ ] 수동 검증: 프로필 정보 조회 → 편집 폼 표시 → 모달 표시

## Verification
```bash
cd /Users/jaehyun/Documents/workspace/tourwave-web
./scripts/verify-task.sh T-013
```
예상: build ✓ / lint ✓ / typecheck ✓

## Rollback
```bash
git revert <commit-hash>
./scripts/task-finish.sh --cancel T-013
```

## Notes
- 시간 표시 (createdAt, updatedAt): 모두 UTC → 사용자 로컬 타임존으로 변환
- 이메일 검증 상태는 배지로 표시 (T-916 디자인 시스템 활용)
- 프로필 이미지는 T-901+ 에셋 관리 완료 후 통합
- 프로필 수정 후 자동 새로고침: T-014에서 success callback 전달
