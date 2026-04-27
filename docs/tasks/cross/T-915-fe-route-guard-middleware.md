---
id: T-915
title: "T-915 — [FE] common — 라우트 가드 middleware.ts (역할 기반 redirect)"
aliases: [T-915]

repo: tourwave-web
area: fe
milestone: cross
domain: common
layer: middleware
size: M
status: backlog

depends_on: []
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-28
updated: 2026-04-28
---

#status/backlog #area/fe

# T-915 — [FE] common — 라우트 가드 middleware.ts

## 파일 소유권
WRITE:
  - `src/middleware.ts` (신규 — Next.js Edge middleware)
  - `src/lib/auth/middlewareGuard.ts` (신규 — 역할 검사 헬퍼)
  - `src/lib/auth/__tests__/middlewareGuard.test.ts` (신규)

READ:
  - `src/lib/auth/storage.ts` (토큰 추출)
  - `src/lib/auth/roleGuard.ts` (역할 매핑 재사용)
  - `src/lib/auth/useAuth.ts`
  - `docs/policies.md` §역할 매트릭스 (BE repo)

DO NOT TOUCH:
  - `src/app/(auth)/` 페이지 구조
  - `src/lib/auth/AuthProvider.tsx` (클라이언트 측 가드 유지)

## SSOT 근거
- `docs/gap-matrix.md` 표 3 cross-cutting: T-915 ❌ (FE 라우트 가드 부재)
- `docs/audit/FE-SUMMARY.md` (있다면): 보호 라우트 미들웨어 부재
- BE `docs/policies.md` §역할 매트릭스: TRAVELER, CONSULTANT, OPERATOR, ADMIN

## 현재 상태 (갭)
- [ ] `src/middleware.ts` 파일 부재 — Next.js App Router 라우트 보호 unscaffolded
- [ ] 비인증 사용자가 보호 라우트(`/me`, `/operator/**`)에 직접 진입 가능 (클라이언트 가드만 존재 → SSR 시 짧은 깜빡임)
- [ ] 역할별 redirect 정책 미정의

## 구현 지침
1. `src/middleware.ts` 생성 — Next.js Edge middleware
2. matcher 설정: `['/me/:path*', '/operator/:path*', '/admin/:path*']` 등 보호 경로
3. 토큰 검사: httpOnly cookie 기반 (또는 Authorization 헤더 forwarding) → 부재 시 `/login?redirect=<path>` 302
4. 역할 검사: JWT payload decode (Edge 호환 — `jose` 라이브러리), 부적합 시 `/forbidden` 302
5. `middlewareGuard.ts`: `requireAuth()`, `requireRole(allowed: Role[])` 헬퍼
6. 단위 테스트: vitest + Next.js Request mock — 토큰 없음/만료/역할 부적합/허용 4 케이스

## Acceptance Criteria
- [ ] `src/middleware.ts` 작성 + matcher 설정
- [ ] 보호 경로 진입 시 비인증 → `/login?redirect=...` 302
- [ ] 역할 부족 시 → `/forbidden` 302
- [ ] httpOnly cookie 토큰 추출 동작
- [ ] `middlewareGuard.test.ts` 4개 이상 PASS
- [ ] `npm test` 회귀 PASS
- [ ] `npm run build` 통과 (Edge runtime 호환)

## Verification
```bash
cd /Users/jaehyun/Documents/workspace/tourwave-web
./scripts/verify-task.sh T-915
```

## Rollback
```bash
cd /Users/jaehyun/Documents/workspace/tourwave-web
rm -f src/middleware.ts
rm -f src/lib/auth/middlewareGuard.ts
rm -f src/lib/auth/__tests__/middlewareGuard.test.ts
npm test
```

## Notes
- Edge runtime 제약: Node.js API 일부 사용 불가 → `jose` 또는 Web Crypto 사용
- httpOnly cookie 이름 BE와 협의 필요 (현재 `tw_access` 가정 — 실제 값 확인 후 설정)
- 클라이언트 가드(`ClientAuthGuard`)는 유지 — middleware는 SSR/edge 보호, ClientAuthGuard는 SPA 전이 보호
