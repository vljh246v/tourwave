# T-008 — [FE] Auth — 로그인 폼 컴포넌트

## Meta
- ID: T-008
- Milestone: M1 (인증·탐색)
- Domain: Auth
- Area: FE
- Layer: ui
- Size: M
- Depends on: T-010 (API 클라이언트), T-912 (인증 모듈)
- Blocks: 없음
- GitHub Issue: #24
- Status: Backlog

## 파일 소유권 (FE 프로젝트 기준 경로)

WRITE:
  - `src/features/auth/LoginForm.tsx`
  - `src/features/auth/LoginForm.test.tsx`

READ:
  - `docs/openapi.yaml` (Auth tag)
  - `docs/policies.md` (인증 정책)
  - `src/lib/api/schema.ts` (T-911 생성 후)
  - `src/lib/auth/` (T-912 생성 후)

DO NOT TOUCH:
  - `src/app/(auth)/login/page.tsx` (T-XXX에서 임포트함)
  - 다른 도메인의 컴포넌트

## SSOT 근거
- `docs/openapi.yaml` → paths `/auth/login` (POST)
  - Request: `LoginRequest` (email, password)
  - Response: `AuthResponse` (accessToken, refreshToken, user)
  - Errors: 401 Unauthorized, 422 Unprocessable

## 현재 상태 (갭)
- [ ] 컴포넌트 미존재 (seed state)
- [ ] `src/features/auth/` 디렉토리 없음
- [ ] API 클라이언트 미완 (T-010 블로커)

## 구현 지침
1. React 19 + TypeScript 함수형 컴포넌트
2. Tailwind v4 스타일링 (PostCSS 플러그인)
3. 폼 필드: 이메일(text), 비밀번호(password), 제출 버튼
4. 입력값 유효성 검사 (이메일 형식, 비밀번호 공백 불가)
5. 로딩 상태 UI (버튼 비활성화, 스피너 표시)
6. 에러 처리: 백엔드 에러 메시지 표시 (401, 422 매핑)
7. T-912의 `useAuth()` 훅 호출 → 토큰 저장 + 리다이렉트
8. Idempotency-Key 헤더 자동 부착 (T-010 클라이언트 담당)

## Acceptance Criteria
- [ ] 컴포넌트 렌더링 테스트 (email/password 입력, 제출 버튼 클릭)
- [ ] 폼 제출 시 `useAuth().login()` 호출 확인
- [ ] 에러 응답 시 에러 메시지 표시
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과
- [ ] 수동 검증: 폼 입력 → 제출 → 토큰 저장됨

## Verification
```bash
cd /Users/jaehyun/Documents/workspace/tourwave-web
./scripts/verify-task.sh T-008
```
예상: build ✓ / lint ✓ / typecheck ✓ / security ✓

## Rollback
```bash
git revert <commit-hash>
# 또는 워크트리 삭제 (진행 중인 경우)
./scripts/task-finish.sh --cancel T-008
```

## Notes
- 디자인 시스템: T-916 (미완) 기본 Input/Button 컴포넌트 의존
- 비밀번호 필드에 보이기/숨기기 토글 권장
- 로그인 성공 후 리다이렉트는 T-912에서 처리
