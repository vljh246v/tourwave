# T-012 — [FE] Auth — 비밀번호 리셋 플로우 UI

## Meta
- ID: T-012
- Milestone: M1 (인증·탐색)
- Domain: Auth
- Area: FE
- Layer: ui
- Size: M
- Depends on: T-010 (API 클라이언트), T-912 (인증 모듈)
- Blocks: 없음
- GitHub Issue: #28
- Status: Backlog

## 파일 소유권 (FE 프로젝트 기준 경로)

WRITE:
  - `src/features/auth/PasswordResetRequest.tsx` — 이메일 입력 폼
  - `src/features/auth/PasswordResetConfirm.tsx` — 새 비밀번호 설정 폼
  - `src/features/auth/PasswordResetRequest.test.tsx`
  - `src/features/auth/PasswordResetConfirm.test.tsx`
  - `src/app/(auth)/reset/page.tsx` — 요청 라우트
  - `src/app/(auth)/reset/confirm/page.tsx` — 확인 라우트

READ:
  - `docs/openapi.yaml` (Auth tag)
  - `docs/policies.md` (인증 정책)
  - `src/lib/api/schema.ts` (T-911 생성 후)

DO NOT TOUCH:
  - T-008, T-009, T-011 인증 컴포넌트
  - 다른 도메인의 컴포넌트

## SSOT 근거
- `docs/openapi.yaml` → paths:
  - `POST /auth/password/reset-request` (비인증)
  - `POST /auth/password/reset-confirm` (비인증, 토큰 필요)
  - Responses: 204 성공, 400 Bad Request (토큰 만료), 422 Unprocessable

## 현재 상태 (갭)
- [ ] 비밀번호 리셋 폼 컴포넌트 미존재
- [ ] reset 라우트 미구성
- [ ] 토큰 기반 확인 플로우 없음

## 구현 지침

### 1. `PasswordResetRequest.tsx` — 이메일 입력
- 목적: 비밀번호 리셋 이메일 발송 요청
- 폼 필드:
  - 이메일(email): 텍스트 입력, 필수
- 동작:
  - `POST /auth/password/reset-request` 호출
  - 성공 시 (204) → 확인 페이지로 네비게이션
  - 상태 UI: 로딩, 성공 메시지, 에러
- 사용자 열거 방지: 요청 성공 여부를 명시적으로 표시하지 않음 (백엔드에서 204 항상 반환)

### 2. `PasswordResetConfirm.tsx` — 비밀번호 설정
- 목적: 리셋 토큰으로 새 비밀번호 설정
- 폼 필드:
  - 리셋 토큰(text): 이메일에서 받은 토큰, 필수
  - 새 비밀번호(password): 최소 8자 권장, 필수
  - 비밀번호 확인(password): 일치 검증
- 동작:
  - 폼 제출 → `POST /auth/password/reset-confirm`
  - 성공 시 (204) → 로그인 페이지로 리다이렉트
  - 400 에러 (토큰 만료) → "토큰이 만료되었습니다. 다시 요청하세요."
  - 422 에러 → "입력값이 유효하지 않습니다."
- 상태 UI: 로딩, 성공, 에러 메시지

### 3. 라우트 구성
- `src/app/(auth)/reset/page.tsx` — `/auth/reset` (이메일 입력)
- `src/app/(auth)/reset/confirm/page.tsx` — `/auth/reset/confirm?token=XXX` (비밀번호 설정)
  - 쿼리 파라미터 `token` 자동 추출 → 폼에 pre-fill

### 4. 네비게이션 흐름
- 이메일 입력 폼 → 제출 → 확인 페이지로 네비게이션
- 확인 페이지 → 토큰 + 새 비밀번호 입력 → 성공 → 로그인 페이지로 리다이렉트

## Acceptance Criteria
- [ ] `PasswordResetRequest` 폼 렌더링 (이메일 입력, 제출 버튼)
- [ ] `POST /auth/password/reset-request` 호출 확인
- [ ] `PasswordResetConfirm` 폼 렌더링 (토큰, 새 비밀번호, 확인)
- [ ] `POST /auth/password/reset-confirm` 호출 확인
- [ ] 비밀번호 확인 필드 불일치 시 제출 불가
- [ ] 400 에러 시 "토큰 만료" 메시지 표시
- [ ] 성공 시 로그인 페이지로 리다이렉트
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과
- [ ] 수동 검증: 이메일 입력 → 토큰 입력 → 성공/실패 흐름

## Verification
```bash
cd /Users/jaehyun/Documents/workspace/tourwave-web
./scripts/verify-task.sh T-012
```
예상: build ✓ / lint ✓ / typecheck ✓

## Rollback
```bash
git revert <commit-hash>
./scripts/task-finish.sh --cancel T-012
```

## Notes
- 백엔드 `/auth/password/reset-request`는 사용자 열거 방지를 위해 이메일 존재 여부와 무관하게 204 반환
- 토큰 유효시간: 백엔드 정책 (보통 24시간)
- 비밀번호 리셋 후 자동 로그인하지 않음 → 사용자 명시적 로그인 필요
