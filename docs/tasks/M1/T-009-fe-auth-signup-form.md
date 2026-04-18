# T-009 — [FE] Auth — 회원가입 폼 컴포넌트

## Meta
- ID: T-009
- Milestone: M1 (인증·탐색)
- Domain: Auth
- Area: FE
- Layer: ui
- Size: M
- Depends on: T-010 (API 클라이언트), T-912 (인증 모듈)
- Blocks: T-011 (이메일 검증)
- GitHub Issue: #25
- Status: Backlog

## 파일 소유권 (FE 프로젝트 기준 경로)

WRITE:
  - `src/features/auth/SignupForm.tsx`
  - `src/features/auth/SignupForm.test.tsx`

READ:
  - `docs/openapi.yaml` (Auth tag)
  - `docs/policies.md` (인증 정책)
  - `src/lib/api/schema.ts` (T-911 생성 후)
  - `src/lib/auth/` (T-912 생성 후)

DO NOT TOUCH:
  - `src/app/(auth)/signup/page.tsx` (T-XXX에서 임포트함)
  - T-008 LoginForm
  - 다른 도메인의 컴포넌트

## SSOT 근거
- `docs/openapi.yaml` → paths `/auth/signup` (POST)
  - Request: `SignupRequest` (email, password, displayName)
  - Response: `AuthResponse` (accessToken, refreshToken, user)
  - Errors: 409 Conflict (이메일 중복), 422 Unprocessable

## 현재 상태 (갭)
- [ ] 컴포넌트 미존재 (seed state)
- [ ] `src/features/auth/` 디렉토리 없음
- [ ] API 클라이언트 미완 (T-010 블로커)

## 구현 지침
1. React 19 + TypeScript 함수형 컴포넌트
2. Tailwind v4 스타일링
3. 폼 필드: 이메일(text), 비밀번호(password), 표시명(text), 약관동의(checkbox), 제출 버튼
4. 유효성 검사:
   - 이메일: 형식 검증, 중복 체크 (409 응답 처리)
   - 비밀번호: 최소 길이(8자 권장), 공백 불가
   - 표시명: 공백 불가, 최소 1자
   - 약관동의: 체크 필수
5. 로딩 상태 UI (버튼 비활성화, 스피너)
6. 에러 처리: 409(이메일 중복 안내), 422(검증 실패) 매핑
7. T-912의 `useAuth()` 훅 → 로그인 수행 또는 T-011로 리다이렉트
8. 비밀번호 보이기/숨기기 토글

## Acceptance Criteria
- [ ] 폼 필드 렌더링 테스트 (email, password, displayName, checkbox, submit 버튼)
- [ ] 필수 필드 공란 시 제출 불가
- [ ] 이메일 중복(409) 응답 시 "이미 가입된 이메일" 메시지 표시
- [ ] 폼 제출 시 `useAuth().signup()` 호출 확인
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과
- [ ] 수동 검증: 폼 입력 → 제출 → 성공/실패 흐름

## Verification
```bash
cd /Users/jaehyun/Documents/workspace/tourwave-web
./scripts/verify-task.sh T-009
```
예상: build ✓ / lint ✓ / typecheck ✓

## Rollback
```bash
git revert <commit-hash>
./scripts/task-finish.sh --cancel T-009
```

## Notes
- 약관 링크는 T-920(정적 페이지)와 함께 진행
- 회원가입 성공 후 이메일 검증 리다이렉트는 T-912에서 관리
- 백엔드 409 응답 시 자동으로 T-011 이메일 검증 플로우로 안내
