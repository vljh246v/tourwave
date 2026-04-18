# T-912 — [FE] 인증 모듈 (JWT storage, token refresh, httpOnly cookie)

## Meta
- ID: T-912
- Milestone: Cross-cutting
- Domain: common
- Area: FE
- Layer: api-client
- Size: M
- Depends on: T-911 (선택, schema.ts 없어도 느슨하게 가능)
- Blocks: T-008, T-009, T-011, T-012, T-013, T-014, T-015, T-018, T-025
- GitHub Issue: —
- Status: Done

## 파일 소유권 (FE 리포 기준)
WRITE:
  - `src/lib/auth/storage.ts` (토큰 저장소 추상화: localStorage vs httpOnly cookie 전략)
  - `src/lib/auth/useAuth.ts` (훅: 로그인, 로그아웃, refresh, 상태)
  - `src/lib/auth/AuthProvider.tsx` (Context + Provider)
  - `src/lib/auth/index.ts` (공개 API export)

READ:
  - `../tourwave/docs/policies.md` §1~3 (역할/권한 매트릭스)
  - `AGENTS.md` §핵심 규칙 (httpOnly cookie 권장, XSS 위험)
  - T-911 `src/lib/api/schema.ts` (로그인/refresh 응답 타입, 선택사항)

DO NOT TOUCH:
  - `src/lib/api/client.ts` (T-911에서 작성) — 여기서 저장소만 사용
  - T-913~T-916 파일

## SSOT 근거
- `../tourwave/docs/policies.md` §1 (Role Model): 역할 매트릭스 → 로그인 후 역할 캐시 필요.
- `AGENTS.md` §핵심 규칙: "인증 토큰은 httpOnly cookie 권장. localStorage 사용 시 XSS 위험."
- 백엔드 JWT 정책 (명시 필요, 예: exp, iat, sub, scopes)

## 현재 상태 (갭)
- [x] 토큰 저장소 미존재 — httpOnly cookie 전략 (CookieTokenStorage) 구현 완료.
- [x] AuthContext / useAuth 훅 미존재 — AuthProvider + useAuth + useAuthGuard 구현 완료.
- [x] refresh token 로직 미존재 — 단일 비행 refresh (inflightRefresh) 구현 완료.
- [x] 권한 캐시 미존재 — MeResponse memberships 캐시 + org 파생값 구현 완료.

## 구현 지침

1. **토큰 저장 전략 (storage.ts)**
   - **권장 (Option A): httpOnly cookie**
     - 백엔드: Set-Cookie 헤더로 httpOnly, Secure, SameSite=Strict 쿠키 발급
     - 프론트: 자동 send (credentials: 'include') — 저장 로직 불필요
     - 이점: XSS 공격 차단, CSRF 자동 완화
     - 제약: SSR 미들웨어 연동 필수 (T-915 route guard)
   - **대안 (Option B): localStorage (XSS 위험)**
     - 프론트: 로그인 응답 access_token → localStorage.setItem('token', ...)
     - 수동 갱신: refresh_token도 저장 후 만료 시 API 호출
     - 주의: XSS 취약점 있음 → helmet, CSP 헤더 강제.
   
   **선택:** Option A 구현. Option B는 주석으로 대체 코드 제공.

2. **인증 컨텍스트 (AuthProvider.tsx)**
   ```typescript
   interface AuthContextType {
     isAuthenticated: boolean;
     user: { id: string; email: string; roles: string[] } | null;
     org: { id: string; role: 'ORG_OWNER' | 'ORG_ADMIN' | 'ORG_MEMBER' } | null;
     loading: boolean;
     error: string | null;
     login: (email: string, password: string) => Promise<void>;
     logout: () => Promise<void>;
     refreshToken: () => Promise<boolean>; // true: 성공, false: 만료
   }
   ```
   - Provider: 트리 최상위 (src/app/layout.tsx 래핑)
   - 초기화: 마운트 시 /me 엔드포인트 호출 (이전 세션 복원)

3. **훅 (useAuth.ts)**
   - `useAuth()` — context 접근, null 체크 포함
   - `useAuthGuard(requiredRoles: string[])` — 권한 검증 (T-915와 연동, 클라이언트 사이드 일차 필터)

4. **Token Refresh 메커니즘**
   - 만료 감지: response 401 → 자동으로 `/auth/refresh` 호출
   - 재시도: refresh 성공 → 원 요청 재실행, 실패 → 로그아웃 + /login 리다이렉트
   - 동시성: 여러 API가 동시에 401 받으면 refresh 한 번만 (race condition 방지)

5. **권한 캐시**
   - 로그인 후 `/me` 응답에 roles, org 정보 캐시
   - 갱신 타이밍: 주기적(5분) 또는 수동 `/auth/refresh` 호출 시
   - 로그아웃: 캐시 초기화 + BE 토큰 무효화

6. **에러 처리**
   - 401 Unauthorized → refresh 시도 → 실패 시 /login으로 리다이렉트
   - 403 Forbidden → 에러 메시지 표시 (권한 부족)
   - 네트워크 오류 → 재시도 (지수 백오프)

7. **TypeScript 타입**
   - 타입은 T-911 `schema.ts`에서 import (없으면 any 허용, 나중에 좁히기)
   - 예: `import { User, Organization } from '@/lib/api/schema'`

## Acceptance Criteria
- [x] `useAuth()` 훅 사용 가능 (로그인, 로그아웃, isAuthenticated 상태)
- [x] `AuthProvider` wrapping 후 context에 접근 가능
- [x] Token refresh 자동화 (401 응답 시 자동 갱신)
- [x] Logout 후 로컬 캐시 초기화 확인
- [x] httpOnly cookie 구현 시: credentials 옵션, middleware.ts 연동 (T-915)
- [x] 권한 매트릭스 문서 주석 (policies.md 참조)

## Verification
```bash
./scripts/verify-task.sh T-912
# 자동 검증:
# 1. src/lib/auth 파일 생성 여부 (4개)
# 2. npm run typecheck 통과 (auth import)
# 3. 기본 로그인/로그아웃 워크플로우 테스트 (T-914 후 통합)
```

## Rollback
```bash
# 저장소 복구
rm -rf src/lib/auth/
# 또는 git checkout
git checkout HEAD -- src/lib/auth/
```

## Notes
- **httpOnly 쿠키 선택 시:** Next.js 13+ App Router + middleware.ts 필수. 백엔드와 쿠키 정책 조율 필요 (도메인, Path, SameSite).
- **역할 캐시:** 매번 /me 호출하지 말 것. 로그인 1회 + 5분 주기 갱신 권장.
- **Depends on 검토:** T-911 없어도 구현 가능 (타입 느슨함). 하지만 T-008+ 태스크는 T-911 필수.
- **보안 체크리스트:** CSP 헤더, CSRF 토큰 (GET 제외), 세션 타임아웃, refresh token 탈취 방지 (httpOnly 강제).
