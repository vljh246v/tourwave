# T-015 — [FE] Me — 계정 비활성화 확인 모달

## Meta
- ID: T-015
- Milestone: M1 (인증·탐색)
- Domain: Me
- Area: FE
- Layer: ui
- Size: S
- Depends on: T-010 (API 클라이언트), T-912 (인증)
- Blocks: 없음
- GitHub Issue: #31
- Status: Backlog

## 파일 소유권 (FE 프로젝트 기준 경로)

WRITE:
  - `src/features/me/DeactivateModal.tsx` — 계정 비활성화 확인 모달
  - `src/features/me/DeactivateModal.test.tsx`

READ:
  - `docs/openapi.yaml` (Me tag)
  - `docs/policies.md` (인증 정책)
  - `src/lib/api/schema.ts` (T-911 생성 후)
  - T-013 profile page (parent 컴포넌트)

DO NOT TOUCH:
  - T-013 ProfileView, T-014 ProfileEditForm
  - 다른 도메인의 컴포넌트

## SSOT 근거
- `docs/openapi.yaml` → paths:
  - `POST /me/deactivate` (인증필수)
  - Request: 없음
  - Response: 204 (비활성화 완료)
  - Errors: 401 Unauthorized

## 현재 상태 (갭)
- [ ] 계정 비활성화 모달 컴포넌트 미존재
- [ ] 비활성화 확인 UI 없음
- [ ] 계정 비활성화 후 로그아웃 플로우 없음

## 구현 지침

### 1. 모달 컴포넌트 (`DeactivateModal.tsx`)
- 목적: 계정 비활성화 전 사용자 확인 및 경고
- 구조:
  - 헤더: "계정 비활성화"
  - 바디: 경고 메시지 + 확인 체크박스
  - 푸터: "취소" 버튼 + "비활성화" 버튼
- 메시지 내용:
  - "계정을 비활성화하면 로그인할 수 없습니다."
  - "기존 예약은 유지되며, 나중에 계정을 다시 활성화할 수 있습니다."(선택)
  - "이 작업은 되돌릴 수 있습니다."
- 확인 체크박스:
  - "계정 비활성화에 동의합니다" (필수, 체크 전 "비활성화" 버튼 비활성화)

### 2. 동작 흐름
1. T-013 profile page에서 "계정 비활성화" 버튼 클릭 → 모달 오픈
2. 사용자 확인 메시지 읽음
3. 체크박스 체크 → "비활성화" 버튼 활성화
4. "비활성화" 버튼 클릭 → `POST /me/deactivate` 호출
5. 성공 시 (204):
   - 모달 닫기
   - T-912에 알림 → 토큰 삭제 + 로그아웃
   - 로그인 페이지로 자동 리다이렉트 + 성공 메시지 표시
6. 실패 시 (401):
   - "인증 필요. 다시 로그인하세요." → 로그인 페이지로 리다이렉트

### 3. 상태 관리
- `isOpen: boolean` — 모달 표시 여부
- `isLoading: boolean` — 요청 중 상태
- `isConfirmed: boolean` — 체크박스 상태
- `error?: string` — 에러 메시지
- Props:
  - `open: boolean` — 모달 오픈 상태
  - `onClose: () => void` — 모달 닫기 콜백
  - `onSuccess?: () => void` — 비활성화 성공 후 콜백 (선택)

### 4. UI 상세
- 모달 스타일: 경고 색상(주황/빨강) 강조
- 버튼:
  - "취소" (secondary) — 모달 닫기
  - "비활성화" (danger/red) — 활성화 조건: 체크박스 체크 + 로딩 아님
- 로딩 상태: 버튼 비활성화 + 스피너
- 에러 메시지: 모달 상단 또는 하단 alert

## Acceptance Criteria
- [ ] 모달 렌더링 확인 (헤더, 메시지, 체크박스, 버튼)
- [ ] 초기 상태: "비활성화" 버튼 비활성화
- [ ] 체크박스 체크 → "비활성화" 버튼 활성화
- [ ] "취소" 버튼 클릭 → 모달 닫기 (onClose 콜백)
- [ ] "비활성화" 버튼 클릭 → `POST /me/deactivate` 호출
- [ ] 성공 시 (204) 모달 닫기 + onSuccess 콜백
- [ ] 401 에러 시 "인증 필요" 메시지 표시
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과
- [ ] 수동 검증: 모달 오픈 → 체크박스 → 비활성화 → 성공/실패 흐름

## Verification
```bash
cd /Users/jaehyun/Documents/workspace/tourwave-web
./scripts/verify-task.sh T-015
```
예상: build ✓ / lint ✓ / typecheck ✓

## Rollback
```bash
git revert <commit-hash>
./scripts/task-finish.sh --cancel T-015
```

## Notes
- 계정 비활성화는 soft delete (물리적 삭제 아님, 복구 가능)
- 비활성화 후 자동 로그아웃: T-912 인증 모듈에서 토큰 삭제
- "계정 삭제" (POST /me/delete)는 별도 모달로 구분 (선택사항, Phase 2)
- 확인 체크박스는 사용자 실수 방지 UX 패턴
