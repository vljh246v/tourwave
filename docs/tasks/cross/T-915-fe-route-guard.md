# T-915 — [FE] 라우트 가드 middleware.ts (역할 기반 리다이렉트)

## Meta
- ID: T-915
- Milestone: Cross-cutting
- Domain: common
- Area: FE
- Layer: middleware
- Size: M
- Depends on: T-912 (AuthProvider 필수)
- Blocks: T-018, T-025 (운영자/보호 라우트)
- GitHub Issue: —
- Status: Backlog

## 파일 소유권 (FE 리포 기준)
WRITE:
  - `src/middleware.ts` 신규 (Next.js middleware, 라우트 가드)
  - `src/lib/auth/roleGuard.ts` 신규 (역할 검증 유틸)
  - `src/app/layout.tsx` (AuthProvider 래핑 확인)

READ:
  - `../tourwave/docs/policies.md` §1~3 (역할/권한 매트릭스)
  - `AGENTS.md` (디렉토리 규칙, httpOnly 쿠키 전략)
  - T-912 `src/lib/auth/` (인증 모듈)

DO NOT TOUCH:
  - `src/app/` 라우트 정의 (T-008+ 태스크에서 정의)
  - T-911~T-914, T-916 파일

## SSOT 근거
- `../tourwave/docs/policies.md` §1~3 — 역할 매트릭스 구현 근거.
  - ORG_OWNER ⊇ ORG_ADMIN ⊇ ORG_MEMBER
  - 비인증 → 403, 권한 부족 → 404 또는 403 (리소스 열거 위험 고려)
- `AGENTS.md` §핵심 규칙 — httpOnly 쿠키 선택 시 middleware에서 쿠키 접근 + 토큰 검증 필수.

## 현재 상태 (갭)
- [ ] middleware.ts 미존재 — 요청 시점 인증 검증 없음.
- [ ] 역할별 라우트 보호 미존재 — /admin, /operator 등 진입 제어 없음.
- [ ] 토큰 만료 시 리다이렉트 로직 미존재.
- [ ] 권한 불충분 시 처리 (403 vs 404) 미정.

## 구현 지침

1. **middleware.ts 신규** (Next.js 13+ App Router)
   ```typescript
   import { NextRequest, NextResponse } from 'next/server';
   
   const PROTECTED_ROUTES = [
     '/bookings',
     '/profile',
     '/admin',
     '/operator',
   ];
   const OPERATOR_ROUTES = ['/admin', '/operator'];
   const ADMIN_ROUTES = ['/admin/members', '/admin/reports'];
   
   export async function middleware(request: NextRequest) {
     const { pathname } = request.nextUrl;
     
     // 1. 쿠키에서 토큰 추출 (httpOnly) 또는 localStorage (프론트 전용)
     const token = request.cookies.get('token')?.value || null;
     
     // 2. 보호 라우트 확인
     const isProtected = PROTECTED_ROUTES.some((route) =>
       pathname.startsWith(route)
     );
     
     if (isProtected && !token) {
       // 비인증 → /login으로 리다이렉트 (returnTo 쿼리 포함)
       return NextResponse.redirect(
         new URL(`/login?returnTo=${pathname}`, request.url)
       );
     }
     
     // 3. 운영자 라우트 체크
     if (OPERATOR_ROUTES.some((route) => pathname.startsWith(route))) {
       // 토큰 검증 필요 (Bearer 헤더 추가 또는 쿠키 기반 자동)
       const isOperator = await verifyOperatorRole(token);
       if (!isOperator) {
         // 권한 부족 → 403 또는 404
         return NextResponse.json(
           { error: 'Insufficient permission' },
           { status: 403 }
         );
       }
     }
     
     // 4. 관리자 라우트 체크
     if (ADMIN_ROUTES.some((route) => pathname.startsWith(route))) {
       const isAdmin = await verifyAdminRole(token);
       if (!isAdmin) {
         return NextResponse.json(
           { error: 'Admin access required' },
           { status: 403 }
         );
       }
     }
     
     return NextResponse.next();
   }
   
   export const config = {
     matcher: [
       // 정적 자산 제외
       '/((?!_next|favicon.ico|public).*)',
     ],
   };
   ```

2. **roleGuard.ts** (역할 검증 유틸)
   ```typescript
   // 토큰 파싱 (JWT 기본 검증)
   export function parseToken(token: string): { sub: string; roles: string[]; org?: string } | null {
     try {
       // JWT 페이로드 디코딩 (서명 검증은 백엔드에서)
       const [, payload] = token.split('.');
       const decoded = JSON.parse(Buffer.from(payload, 'base64').toString());
       return { sub: decoded.sub, roles: decoded.roles || [] };
     } catch {
       return null;
     }
   }
   
   export async function verifyOperatorRole(token: string | null): Promise<boolean> {
     if (!token) return false;
     const decoded = parseToken(token);
     return decoded?.roles?.includes('ORG_ADMIN') ||
            decoded?.roles?.includes('ORG_OWNER') || false;
   }
   
   export async function verifyAdminRole(token: string | null): Promise<boolean> {
     if (!token) return false;
     const decoded = parseToken(token);
     return decoded?.roles?.includes('ORG_OWNER') || false;
   }
   ```

3. **라우트 보호 계층 (3단계)**
   - **비인증:** /login, /signup, 공개 페이지 (투어 목록, 상세)
   - **인증 필수:** /bookings, /profile, /favorites
   - **운영자:** /operator/* (예약 관리, 발생 관리)
   - **관리자:** /admin/* (멤버, 리포트, 조정)

4. **Client-side 보완 (useAuth + 조기 종료)**
   - middleware는 서버 단계 (1차 방어)
   - 클라이언트: `useAuth()` 훅으로 권한 재확인 (2차 방어)
   - 이유: middleware 우회 불가능 + 클라이언트 캐시 일관성 유지

5. **에러 처리 및 리다이렉트**
   - 401 (비인증) → /login?returnTo=/path
   - 403 (권한 부족) → /403 또는 /home
   - 404 (리소스 없음) → /404 (민감한 리소스는 404 표시)

6. **httpOnly 쿠키 전략** (T-912 선택 시)
   - 백엔드: Set-Cookie: token=...; HttpOnly; Secure; SameSite=Strict
   - 프론트: request.cookies.get('token') (middleware에서만 접근 가능)
   - 프론트 클라이언트 컴포넌트: 쿠키 접근 불가 → AuthContext로 상태 공유

7. **설정 (matcher)**
   - /_next/* 및 정적 자산 제외 (성능)
   - 공개 라우트(/login, /signup, /tours) 제외 (선택적, middleware 내 로직으로도 가능)

## Acceptance Criteria
- [ ] `src/middleware.ts` 생성 (matcher + 역할 검증 로직)
- [ ] `src/lib/auth/roleGuard.ts` 생성 (parseToken, verify* 함수)
- [ ] /login, /signup 공개 라우트 접근 가능
- [ ] /bookings, /profile 보호 라우트 비인증 시 /login으로 리다이렉트
- [ ] /operator/* 운영자 권한 검증 (부족 시 403)
- [ ] /admin/* 관리자 권한 검증 (부족 시 403)
- [ ] AuthProvider와 middleware 연동 (App Router layout에서 확인)
- [ ] npm run typecheck 통과

## Verification
```bash
./scripts/verify-task.sh T-915
# 자동 검증:
# 1. src/middleware.ts 존재
# 2. src/lib/auth/roleGuard.ts 존재
# 3. npm run typecheck 통과
# 4. 라우트 설정 검증 (matcher 문법 확인)
# 5. 기본 리다이렉트 시뮬레이션 (테스트 또는 수동)
```

## Rollback
```bash
# 파일 제거
rm src/middleware.ts src/lib/auth/roleGuard.ts
# 또는 git 복구
git checkout HEAD -- src/middleware.ts src/lib/auth/roleGuard.ts
```

## Notes
- **Next.js 미들웨어 제약:** 동적 import 제한 있음. 복잡한 로직은 외부 함수로 분리 권장.
- **JWT 검증:** middleware에서는 페이로드만 디코딩 (서명 검증은 백엔드에서). 보안을 위해 민감한 작업은 클라이언트에서 재검증.
- **성능:** middleware는 모든 요청 통과 → 빠를 것. 토큰 파싱은 가볍고, API 호출은 제외.
- **Depends on T-912:** AuthProvider 없으면 클라이언트 보완 계층 약함. 반드시 T-912 먼저.
- **운영자/관리자 라우트 정의:** 구체 경로는 T-018, T-025에서 정의. 여기는 템플릿만 제공.
- **CSRF 방지:** httpOnly 쿠키 + SameSite=Strict 조합. 추가 CSRF 토큰은 상태 변경 요청만 (T-911 gen:api에서 자동 부착).
