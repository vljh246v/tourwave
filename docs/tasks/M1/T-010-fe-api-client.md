# T-010 — [FE] API Client — API 클라이언트 + 에러 처리

## Meta
- ID: T-010
- Milestone: M1 (인증·탐색)
- Domain: API Layer
- Area: FE
- Layer: api-client
- Size: L
- Depends on: T-911 (schema.ts 생성), T-912 (인증 모듈)
- Blocks: T-008, T-009, T-011, T-012, T-013, T-014, T-015 (모든 FE 피처)
- GitHub Issue: #26
- Status: Done

## 파일 소유권 (FE 프로젝트 기준 경로)

WRITE:
  - `src/lib/api/client.ts` — fetch 래퍼, Bearer 토큰, Idempotency-Key
  - `src/lib/api/errors.ts` — 에러 정규화, 백엔드 에러코드 매핑
  - `src/lib/api/index.ts` — 공용 export

READ:
  - `docs/openapi.yaml` (모든 엔드포인트)
  - `docs/policies.md` (Idempotency, 에러코드)
  - `src/lib/api/schema.ts` (T-911 생성 후)
  - `src/lib/auth/` (T-912 생성 후)

DO NOT TOUCH:
  - `src/features/` — 아직 생성 전
  - `src/app/` — 아직 생성 전

## SSOT 근거
- `docs/openapi.yaml` → 모든 paths, schemas, error responses
- `docs/policies.md` → section 4.3 (Idempotency Policy), 4.7 (Error Code Supplements)
- 백엔드 에러코드: `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD`, `INVALID_BOOKING_STATE` 등

## 현재 상태 (갭)
- [ ] `src/lib/api/` 디렉토리 없음
- [ ] fetch 래퍼 미존재
- [ ] 에러 정규화 로직 없음
- [ ] Idempotency-Key 헤더 자동 부착 구조 없음

## 구현 지침

### 1. `client.ts` — HTTP 래퍼
- 기본 URL: 환경변수 `NEXT_PUBLIC_API_URL` (fallback: `https://api.example.com`)
- 모든 요청에 `Authorization: Bearer <token>` 자동 부착 (T-912 토큰에서)
- Idempotency-Key 헤더:
  - POST/PATCH/DELETE 요청에 자동 생성 (UUID v4)
  - 상태변경 엔드포인트 필수 (docs/policies.md 4.3 참조)
  - body 해시 포함 (중복 감지용)
- 요청 타입: `fetch(url, { method, headers, body })` + JSON 직렬화
- 응답: 200-299 성공, 그 외 에러 → `errors.ts`로 정규화
- 네트워크 에러 처리: 재시도 로직(exponential backoff, max 3회) 선택사항

### 2. `errors.ts` — 에러 정규화
- 백엔드 에러코드 → 사용자 친화 메시지 매핑
  - 401: "인증 필요. 다시 로그인하세요."
  - 403: "접근 권한이 없습니다."
  - 404: "요청한 리소스를 찾을 수 없습니다."
  - 409: 코드 특정 (INVALID_BOOKING_STATE, OFFER_EXPIRED 등)
  - 422: "입력값이 유효하지 않습니다."
- 구조: `ApiError { statusCode, code, message, details? }`
- 401 응답 시 T-912에 알림 → 토큰 갱신 또는 로그아웃

### 3. `index.ts` — 공용 export
```typescript
export { ApiClient } from './client';
export { ApiError, mapErrorCode } from './errors';
export type { ApiResponse } from './schema';
```

## Acceptance Criteria
- [ ] 기본 GET/POST/PATCH/DELETE 요청 메서드 구현
- [ ] Authorization 헤더 자동 부착 확인 (T-912 토큰과 통합)
- [ ] Idempotency-Key 자동 생성 + UUID v4 형식
- [ ] 400-599 에러 응답 시 `ApiError` 객체 반환
- [ ] 401 응답 시 T-912 토큰 갱신/로그아웃 트리거
- [ ] 에러코드 매핑 테스트 (409 INVALID_BOOKING_STATE, 422 UNPROCESSABLE 등)
- [ ] `npm run typecheck` 통과
- [ ] `npm run lint` 통과
- [ ] 모의 API 호출 테스트 (구조 검증)

## Verification
```bash
cd /Users/jaehyun/Documents/workspace/tourwave-web
./scripts/verify-task.sh T-010
```
예상: build ✓ / lint ✓ / typecheck ✓ / security ✓

## Rollback
```bash
git revert <commit-hash>
./scripts/task-finish.sh --cancel T-010
```

## Notes
- **이 태스크는 모든 후속 FE 피처의 블로커** — 병렬 진행 불가
- T-911 (schema.ts)에서 생성된 타입 import → 타입 안전 API 호출
- httpOnly cookie 토큰 저장 권장 (CSRF 완화, XSS 방어)
- 토큰 갱신 로직(refresh token 자동 갱신)은 T-912에서 처리
