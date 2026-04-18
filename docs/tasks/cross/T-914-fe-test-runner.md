# T-914 — [FE] 테스트 러너 도입 (vitest + MSW)

## Meta
- ID: T-914
- Milestone: Cross-cutting
- Domain: common
- Area: FE
- Layer: test
- Size: L
- Depends on: 없음
- Blocks: T-008~T-027 (모든 FE 태스크 테스트 필요)
- GitHub Issue: —
- Status: Backlog

## 파일 소유권 (FE 리포 기준)
WRITE:
  - `vitest.config.ts` 신규
  - `src/test-setup.ts` 신규 (MSW, Vitest 훅)
  - `src/__tests__/` (테스트 디렉토리 구조)
  - `package.json` (scripts, devDependencies 업데이트)
    - scripts.test: `vitest`
    - scripts."test:ui": `vitest --ui`
  - `scripts/validators/02-test.sh` 업데이트 (test 실행 + 커버리지)

READ:
  - `AGENTS.md` (테스트 규율 — 아직 명시 필요)
  - `../tourwave/docs/testing.md` (백엔드 테스트 패턴 참고)

DO NOT TOUCH:
  - T-911~T-913 파일 (테스트에서만 import)
  - T-915~T-916 파일

## SSOT 근거
- 프로젝트 필수 인프라 — 테스트 없이 신뢰할 수 없음.
- **Vitest 선택 이유:**
  - Vite 기반 (Next.js 개발 서버와 호환)
  - Jest 호환 API → 마이그레이션 용이
  - TypeScript 우수 지원
  - ESM + Node.js 동시 지원
- **MSW (Mock Service Worker) 이유:**
  - API 모킹 표준 (Jest mocks 부재, 실제 fetch/XHR 가로챔)
  - 실제 HTTP 의미론 보존
  - 서버 상태 검증 가능

## 현재 상태 (갭)
- [x] `package.json`에 test script 존재 (but 더미, "exit 0")
- [ ] vitest 미도입.
- [ ] MSW 미도입.
- [ ] test-setup 미존재 (환경 초기화).
- [ ] 테스트 디렉토리 구조 미정.

## 구현 지침

1. **라이브러리 설치**
   ```bash
   npm install --save-dev vitest @vitest/ui msw
   npm install --save-dev @testing-library/react @testing-library/jest-dom
   npm install --save-dev jsdom # 또는 node 환경 선택
   ```

2. **vitest.config.ts 신규**
   ```typescript
   import { defineConfig } from 'vitest/config';
   import react from '@vitejs/plugin-react'; // 필요 시 추가
   
   export default defineConfig({
     plugins: [react()],
     test: {
       globals: true,
       environment: 'jsdom', // 또는 node (MSW는 둘 다 지원)
       setupFiles: './src/test-setup.ts',
       coverage: {
         provider: 'v8',
         reporter: ['text', 'json', 'html'],
         include: ['src/**/*.ts', 'src/**/*.tsx'],
         exclude: ['src/**/*.d.ts', 'src/**/__tests__/**'],
       },
     },
   });
   ```

3. **src/test-setup.ts 신규** (MSW + 전역 setup)
   ```typescript
   import { afterAll, afterEach, beforeAll } from 'vitest';
   import { setupServer } from 'msw/node';
   import { http, HttpResponse } from 'msw';
   import '@testing-library/jest-dom';
   
   // MSW 서버 정의
   export const server = setupServer(
     http.post('/api/auth/login', () =>
       HttpResponse.json({
         accessToken: 'fake-jwt-token',
         user: { id: '1', email: 'test@example.com' },
       }, { status: 200 })
     ),
     // 추가 핸들러...
   );
   
   // 전역 훅
   beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
   afterEach(() => server.resetHandlers());
   afterAll(() => server.close());
   ```

4. **package.json 업데이트**
   ```json
   "scripts": {
     "test": "vitest",
     "test:ui": "vitest --ui",
     "test:run": "vitest run",
     "test:coverage": "vitest run --coverage"
   }
   ```

5. **테스트 디렉토리 구조** (src/\_\_tests\_\_/)
   ```
   src/__tests__/
     lib/
       api/
         client.test.ts (T-911 fetch wrapper)
       auth/
         useAuth.test.ts (T-912 훅)
       state/
         bookingStore.test.ts (T-913)
     components/
       Button.test.tsx (T-916)
   ```

6. **테스트 규율**
   - **단위 테스트** (lib/ 중심)
     - 순수 함수: 입력 → 출력
     - 훅: renderHook 활용 (@testing-library/react)
     - 저장소: store 직접 호출 + 액션 검증
   - **통합 테스트** (components/ + features/)
     - 컴포넌트 렌더링 + 사용자 인터랙션
     - MSW 모킹 API
     - 상태 흐름 검증
   - **커버리지 목표:** 70% 라인 커버리지 (MVP), 진전 시 80% 목표

7. **실행 예시**
   ```bash
   npm test                    # watch mode
   npm run test:run            # 단일 실행
   npm run test:coverage       # 커버리지 리포트
   npm run test:ui             # UI 대시보드
   ```

8. **CI 통합**
   - validator: `scripts/validators/02-test.sh`
   - 명령어: `npm run test:run && npm run test:coverage`
   - 실패 시: exit code > 0

## Acceptance Criteria
- [ ] `npm install vitest @testing-library/react msw` 성공
- [ ] `vitest.config.ts` 생성 (jsdom 환경, setupFiles 포함)
- [ ] `src/test-setup.ts` 생성 (MSW 서버 + beforeAll/afterEach)
- [ ] `npm test` 실행 가능 (watch mode)
- [ ] `npm run test:run` 통과 (테스트 존재 시)
- [ ] 최소 3개 테스트 작성 (auth, store, component 각 1개)
- [ ] 커버리지 리포트 생성 (html 형식)
- [ ] GitHub Actions 또는 CI에서 테스트 자동 실행 설정 (선택)

## Verification
```bash
./scripts/verify-task.sh T-914
# 자동 검증:
# 1. vitest.config.ts 존재
# 2. src/test-setup.ts 존재
# 3. npm run test:run 통과
# 4. npm run test:coverage 커버리지 리포트 생성
# 5. 최소 3개 테스트 케이스 존재 확인
```

## Rollback
```bash
# 라이브러리 제거
npm uninstall vitest @vitest/ui msw @testing-library/react
# 파일 제거
rm vitest.config.ts src/test-setup.ts
# 테스트 디렉토리 제거
rm -rf src/__tests__/
# package.json 수동 복구
```

## Notes
- **MSW 핸들러 관리:** handlers를 별도 파일로 분리 (src/test-setup/handlers.ts) 권장. 나중 개수 증가 시.
- **Server Component 테스트:** Next.js 문제 — 클라이언트 컴포넌트만 쉽게 테스트 가능. Server Component는 E2E 테스트 또는 integration 계획 필수.
- **비동기 테스트:** `async/await` 또는 `waitFor()` 사용 (Testing Library).
- **모킹 전략:** 실제 백엔드 없이 MSW로 모킹. 나중 E2E(Cypress, Playwright)는 별도 태스크.
- **DevTools 통합:** Vitest UI (`npm run test:ui`)는 개발 중 유용. CI에서는 불필요.
- **Depends on 재검토:** T-914는 모든 FE 테스트 태스크를 Blocks. 우선 작업 권장.
