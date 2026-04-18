# T-011 — [FE] Auth — 이메일 검증 플로우 UI

## Meta
- ID: T-011
- Milestone: M1 (인증·탐색)
- Domain: Auth
- Area: FE
- Layer: ui
- Size: M
- Depends on: T-010 (API 클라이언트), T-009 (회원가입), T-912 (인증 모듈)
- Blocks: 없음
- GitHub Issue: #27
- Status: Backlog

## 파일 소유권 (FE 프로젝트 기준 경로)

WRITE:
  - `src/features/auth/EmailVerification.tsx` — 이메일 검증 폼
  - `src/features/auth/EmailVerification.test.tsx`
  - `src/app/(auth)/verify/page.tsx` — 라우트

READ:
  - `docs/openapi.yaml` (Auth tag)
  - `docs/policies.md` (인증 정책)
  - `src/lib/api/schema.ts` (T-911 생성 후)
  - `src/lib/auth/` (T-912 생성 후)

DO NOT TOUCH:
  - T-008, T-009 LoginForm, SignupForm
  - 다른 도메인의 컴포넌트

## SSOT 근거
- `docs/openapi.yaml` → paths:
  - `POST /auth/email/verify-request` (인증된 사용자)
  - `POST /auth/email/verify-confirm` (비인증, 토큰 필요)
  - Responses: 204 성공, 400 Bad Request (토큰 만료/무효), 422 Unprocessable

## 현재 상태 (갭)
- [ ] 컴포넌트 미존재 (seed state)
- [ ] 이메일 검증 페이지 라우트 없음
- [ ] 검증 토큰 입력 UI 없음

## 구현 지침

### 1. 폼 컴포넌트 (`EmailVerification.tsx`)
- 두 단계 플로우:
  - **Step 1 (요청):** "검증 이메일 재전송" 버튼 → `POST /auth/email/verify-request`
  - **Step 2 (확인):** 이메일에서 받은 토큰 입력 → `POST /auth/email/verify-confirm`
- UI 요소:
  - 이메일 표시 (읽기전용)
  - 검증 코드 입력 필드 (6자리 숫자 또는 토큰)
  - "코드 다시 받기" 버튼 (재전송, 쿨다운 60초)
  - 제출 버튼
- 로딩/에러 상태: 버튼 비활성화, 스피너, 에러 메시지
- 400 에러 → "검증 코드가 만료되었습니다. 다시 요청하세요."
- 422 에러 → "입력값이 유효하지 않습니다."

### 2. 라우트 (`src/app/(auth)/verify/page.tsx`)
- 쿼리 파라미터 지원: `?email=user@example.com&step=confirm` (선택사항)
- 검증 미완료 사용자만 접근 → 미인증 사용자 리다이렉트 처리
- 검증 완료 시 홈(/main) 또는 대시보드로 자동 리다이렉트

### 3. 시간 정책
- 재전송 쿨다운: 60초 (UI 카운트다운 타이머)
- 토큰 만료: 백엔드 정책 (docs/domain-rules.md 참조)
- 모든 타임스탐프는 UTC → 표시 시 occurrence 타임존 변환(이 플로우에서는 불필요)

## Acceptance Criteria
- [ ] "검증 이메일 재전송" 버튼 클릭 → `POST /auth/email/verify-request` 호출
- [ ] 검증 코드 입력 + 제출 → `POST /auth/email/verify-confirm` 호출
- [ ] 400 에러 시 사용자 친화 메시지 표시
- [ ] 재전송 쿨다운 타이머 동작 확인
- [ ] 검증 완료 시 홈으로 리다이렉트
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과
- [ ] 수동 검증: 코드 입력 → 제출 → 성공/실패 흐름

## Verification
```bash
cd /Users/jaehyun/Documents/workspace/tourwave-web
./scripts/verify-task.sh T-011
```
예상: build ✓ / lint ✓ / typecheck ✓

## Rollback
```bash
git revert <commit-hash>
./scripts/task-finish.sh --cancel T-011
```

## Notes
- 검증 코드 형식(숫자/토큰)은 백엔드 결정 → openapi.yaml 참조
- 이메일 발송은 백엔드 담당 (verify-request 호출 시)
- 검증 상태는 T-912 인증 모듈에서 관리 (user.emailVerified 플래그)
