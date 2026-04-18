# T-014 — [FE] Me — 프로필 수정 폼

## Meta
- ID: T-014
- Milestone: M1 (인증·탐색)
- Domain: Me
- Area: FE
- Layer: ui
- Size: M
- Depends on: T-010 (API 클라이언트), T-913 (상태 관리), T-912 (인증)
- Blocks: 없음
- GitHub Issue: #30
- Status: Backlog

## 파일 소유권 (FE 프로젝트 기준 경로)

WRITE:
  - `src/features/me/ProfileEditForm.tsx` — 프로필 수정 폼
  - `src/features/me/ProfileEditForm.test.tsx`

READ:
  - `docs/openapi.yaml` (Me tag)
  - `docs/policies.md` (인증 정책)
  - `src/lib/api/schema.ts` (T-911 생성 후)
  - T-013 profile page (parent 컴포넌트)

DO NOT TOUCH:
  - T-013 ProfileView (읽기전용)
  - T-015 DeactivateModal
  - 다른 도메인의 컴포넌트

## SSOT 근거
- `docs/openapi.yaml` → paths:
  - `PATCH /me` (인증필수)
  - Request: `MeUpdateRequest` (displayName, profileImageUrl 등)
  - Response: `User` (업데이트된 사용자 정보)
  - Errors: 401 Unauthorized, 422 Unprocessable

## 현재 상태 (갭)
- [ ] 프로필 수정 폼 컴포넌트 미존재
- [ ] 폼 유효성 검사 로직 없음
- [ ] 모달/드로어 컴포넌트 미구성

## 구현 지침

### 1. 폼 컴포넌트 (`ProfileEditForm.tsx`)
- 목적: 사용자 프로필 정보 수정
- 폼 필드:
  - 표시명 (displayName, text): 현재값 pre-fill, 최소 1자, 필수
  - 프로필 이미지 (선택, T-901+ 완료 후 통합)
- 모달/드로어 구조:
  - 헤더: "프로필 수정"
  - 바디: 폼 필드
  - 푸터: "취소" 버튼 + "저장" 버튼
- 동작:
  - 폼 오픈 시 현재 데이터 로드 (T-013에서 전달하거나 `GET /me` 재호출)
  - 수정 후 제출 → `PATCH /me` 호출
  - 성공 시 (200) → 모달 닫기 + 부모(T-013)에 콜백 전달 → ProfileView 새로고침
  - 실패 시 (422) → 에러 메시지 표시
- 상태 UI: 로딩, 에러, 성공

### 2. 폼 유효성 검사
- 표시명: 공백 불가, 최소 1자, 최대 50자(권장)
- 변경사항 없음 → "저장" 버튼 비활성화
- 요청 중 → 버튼 비활성화 + 스피너

### 3. 에러 처리
- 401: "인증 필요. 다시 로그인하세요." → 로그인 페이지로 리다이렉트
- 422: "입력값이 유효하지 않습니다." + 필드별 메시지 표시
- 네트워크 에러: "저장 실패. 다시 시도하세요."

### 4. Idempotency
- `PATCH /me` 호출 시 자동으로 Idempotency-Key 부착 (T-010 클라이언트)
- 중복 제출 방지: 버튼 비활성화

## Acceptance Criteria
- [ ] 폼 필드 렌더링 (displayName, 저장/취소 버튼)
- [ ] 현재값 pre-fill 확인
- [ ] 폼 제출 → `PATCH /me` 호출 확인
- [ ] 성공 시 (200) 모달 닫기 + 부모 콜백 호출
- [ ] 422 에러 시 에러 메시지 표시
- [ ] 변경사항 없음 → "저장" 버튼 비활성화
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과
- [ ] 수동 검증: 폼 수정 → 제출 → 성공/실패 흐름

## Verification
```bash
cd /Users/jaehyun/Documents/workspace/tourwave-web
./scripts/verify-task.sh T-014
```
예상: build ✓ / lint ✓ / typecheck ✓

## Rollback
```bash
git revert <commit-hash>
./scripts/task-finish.sh --cancel T-014
```

## Notes
- T-013 `profile/page.tsx`에서 이 컴포넌트를 모달/드로어로 임포트
- 성공 콜백: `onSuccess?: () => void` props로 T-013에 ProfileView 새로고침 트리거
- 프로필 이미지 업로드는 T-901+ (에셋 관리) 완료 후 통합
- 표시명 변경은 T-912 인증 모듈의 사용자 상태도 동시 업데이트
