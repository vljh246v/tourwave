# T-913 — [FE] 상태 관리 라이브러리 도입 (Zustand 권장)

## Meta
- ID: T-913
- Milestone: Cross-cutting
- Domain: common
- Area: FE
- Layer: state
- Size: M
- Depends on: 없음
- Blocks: T-014, T-019, T-020~T-027 (예약, 검색, 필터 상태)
- GitHub Issue: —
- Status: Backlog

## 파일 소유권 (FE 리포 기준)
WRITE:
  - `src/lib/state/` 신규 디렉토리
    - `bookingStore.ts` (예약 프로세스 상태)
    - `searchStore.ts` (검색 필터, 정렬, 페이지네이션)
    - `uiStore.ts` (모달, 알림, 로딩)
    - `store.ts` (통합 export)
  - `package.json` (zustand 추가, "zustand": "^5.x.x")

READ:
  - `AGENTS.md` §디렉토리 규칙 (의존 방향: components → lib)
  - 없음 (프로젝트 필수 인프라)

DO NOT TOUCH:
  - `src/features/` (도메인 로직은 나중, 상태 저장소는 여기만)
  - T-912 인증 모듈 (분리된 Context)

## SSOT 근거
- 프로젝트 필수 인프라 — 상태 관리 라이브러리 없으면 prop drilling, 비동기 처리 복잡 증가.
- **Zustand 선택 이유:**
  - 가볍다 (2KB gzip) — tree-shaking 우수
  - 보일러플레이트 최소 — Redux 불필요
  - Next.js App Router + Server Component 친화적
  - TypeScript 지원 우수
  - DevTools 통합 가능 (선택)

## 현재 상태 (갭)
- [ ] 상태 관리 라이브러리 미도입.
- [ ] prop drilling 위험 — 검색 필터 등 여러 계층에서 필요한 상태 분산.
- [ ] 비동기 상태 (로딩, 에러) 처리 전략 미정.

## 구현 지침

1. **라이브러리 설치**
   ```bash
   npm install zustand
   ```

2. **저장소 구조 (src/lib/state/)**
   
   **bookingStore.ts** (예약 프로세스 상태)
   ```typescript
   interface BookingState {
     currentOccurrenceId: string | null;
     partySize: number;
     selectedParticipants: string[]; // 초대된 참가자 ID
     loading: boolean;
     error: string | null;
   }
   interface BookingActions {
     setOccurrence: (id: string) => void;
     setPartySize: (size: number) => void;
     addParticipant: (id: string) => void;
     removeParticipant: (id: string) => void;
     reset: () => void;
   }
   export const useBookingStore = create<BookingState & BookingActions>((set) => ({ ... }))
   ```
   
   **searchStore.ts** (검색 필터, 정렬)
   ```typescript
   interface SearchState {
     query: string;
     filters: { category?: string; priceRange?: [number, number]; dateFrom?: string };
     sortBy: 'relevance' | 'price' | 'rating';
     page: number;
     pageSize: number;
     loading: boolean;
     results: any[];
     totalCount: number;
   }
   interface SearchActions {
     setQuery: (q: string) => void;
     setFilter: (key: string, value: any) => void;
     setSortBy: (sort: string) => void;
     setPage: (p: number) => void;
     reset: () => void;
   }
   export const useSearchStore = create<SearchState & SearchActions>((set) => ({ ... }))
   ```
   
   **uiStore.ts** (전역 UI 상태)
   ```typescript
   interface UIState {
     isModalOpen: boolean;
     modalType: 'booking' | 'review' | 'inquiry' | null;
     notification: { type: 'success' | 'error' | 'info'; message: string } | null;
     sidebarOpen: boolean;
   }
   interface UIActions {
     openModal: (type: string) => void;
     closeModal: () => void;
     showNotification: (type: string, message: string) => void;
     toggleSidebar: () => void;
   }
   ```
   
   **store.ts** (통합 export)
   ```typescript
   export { useBookingStore };
   export { useSearchStore };
   export { useUIStore };
   ```

3. **비동기 상태 처리 (선택사항: middleware)**
   - Zustand 기본 미들웨어로 충분 (actions에서 async 호출)
   - 복잡한 경우: devtools 미들웨어 + immer (나중)

4. **컴포넌트 사용**
   ```typescript
   // 클라이언트 컴포넌트 필수
   'use client';
   import { useBookingStore } from '@/lib/state/store';
   
   export function BookingForm() {
     const { partySize, setPartySize, loading } = useBookingStore();
     return (
       <input value={partySize} onChange={(e) => setPartySize(+e.target.value)} />
     );
   }
   ```

5. **인증 상태와의 분리**
   - AuthContext (T-912) — 로그인 정보, 권한 (불변성 중요)
   - Zustand stores — 페이지 상태, 검색, 필터, 임시 선택 (자주 변경)
   - 이유: 인증 상태는 Context로 충분하고, 나머지는 Zustand로 간결.

6. **TypeScript 정확도**
   - 모든 store에 명시적 타입 지정 (generic 활용)
   - createStore 래퍼 생각 (보일러플레이트 감소, 선택사항)

7. **개발 도구 (선택사항)**
   - DevTools 미들웨어 추가 → React DevTools와 시간 여행 디버깅
   - 나중 단계 (MVP에서는 필수 아님)

## Acceptance Criteria
- [ ] `npm install zustand` 통과
- [ ] `src/lib/state/` 4개 파일 생성 (booking, search, ui, store)
- [ ] 각 store에 최소 3개 action + 5개 state 속성
- [ ] TypeScript strict 모드 통과 (useBookingStore, useSearchStore, useUIStore)
- [ ] 컴포넌트에서 훅 import 후 상태/액션 사용 가능 (단순 테스트)
- [ ] Server Component / Client Component 경계 주석 명시

## Verification
```bash
./scripts/verify-task.sh T-913
# 자동 검증:
# 1. src/lib/state 디렉토리 및 4개 파일 존재
# 2. npm install zustand 성공
# 3. npm run typecheck 통과
# 4. store 기본 호출 테스트 (CLI 또는 간단한 컴포넌트)
```

## Rollback
```bash
# 저장소 제거 + 라이브러리 제거
rm -rf src/lib/state/
npm uninstall zustand
# package.json 수동 복구 (git)
```

## Notes
- **Server Component 제약:** 저장소는 클라이언트 전용. 'use client' 반드시 명시.
- **Props drilling 회피:** 깊이 3 이상이면 Zustand 사용 권장 (Context는 성능 이유로 피함).
- **동시성 이슈:** Zustand는 기본 thread-safe하지 않음 (JS는 싱글 스레드). 웹워커 사용 시만 주의.
- **상태 영속성:** 로컬스토리지 sync는 T-914 후 선택사항 (zustand-persist 미들웨어).
- **Test-ability:** store는 훅 외부에서 직접 사용 가능 → 단위 테스트 용이 (T-914).
