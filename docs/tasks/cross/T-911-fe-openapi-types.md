# T-911 — [FE] OpenAPI 타입 생성 운영화

## Meta
- ID: T-911
- Milestone: Cross-cutting
- Domain: common
- Area: FE
- Layer: tooling
- Size: S
- Depends on: 없음
- Blocks: T-010, T-008, T-009, T-011, T-012, T-013, T-014, T-015, T-018, T-025
- GitHub Issue: —
- Status: Backlog

## 파일 소유권 (FE 리포 기준)
WRITE:
  - `src/lib/api/schema.ts` (생성물; `.gitignore`에서 제외 검토)
  - `scripts/gen-api.sh` (또는 package.json scripts 통합)
  - `package.json` (scripts 업데이트: `gen:api`)

READ:
  - `../tourwave/docs/openapi.yaml` (백엔드 SSOT)
  - `AGENTS.md` (API 계약 규칙)

DO NOT TOUCH:
  - 다른 FE 태스크 파일 (T-912~T-916은 이 타입을 READ만 함)

## SSOT 근거
- `../tourwave/docs/openapi.yaml` — 백엔드 SSOT. FE는 절대 수동 타입 작성 금지.
- `AGENTS.md` §API 계약 — `npm run sync:api && npm run gen:api` 워크플로우 정의.
- `openapi-typescript` 라이브러리 (package.json 기 설정).

## 현재 상태 (갭)
- [x] 기초 설정 — `package.json`에 `openapi-typescript` + `gen:api` 스크립트 존재.
- [ ] `scripts/gen-api.sh` 미존재 또는 최소화 — 스크립트 강화 필요.
- [ ] `src/lib/api/schema.ts` 빌드 타입 일관성 검증 미비 — 타입 진실성 가드 필수.

## 구현 지침

1. **스크립트 강화 (`scripts/gen-api.sh` 또는 `package.json` scripts)**
   - `npm run sync:api` 실행 (백엔드 ../tourwave에서 openapi.yaml 복사)
   - `npm run gen:api` 실행 (openapi-typescript로 타입 생성)
   - 에러 처리: 실패 시 exit code > 0 반환
   - CI/CD 용 --check-only 플래그 고려

2. **gitignore 정책 결정**
   - Option A: `src/lib/api/schema.ts` 를 git 추적 → 매번 커밋 시 타입 갱신
   - Option B: `.gitignore`에서 제외 → 빌드 타임에 생성 (권장, 1-click 재생성 가능)
   - **권장:** Option A (커밋하기). 이유: CI/CD 명확성, 코드리뷰 비교 가능.

3. **진실성 가드 (검증기)**
   - `scripts/validators/02-api.sh` 신규 (또는 기존 확장)
   - 규칙: 생성된 `schema.ts`에 모든 OpenAPI 경로·메서드·응답이 TS 타입으로 반영되었는지 검증.
   - 도구: `openapi-typescript --check` 또는 generate-후-diff.
   - 실패 시: "OpenAPI drift detected" 메시지 + 재생성 안내.

4. **HttpClient 래퍼 가이드** (T-008 이상을 위해)
   - `src/lib/api/client.ts` (신규): fetch 기반 HTTP 클라이언트
   - 자동 기능:
     - `Idempotency-Key` 헤더 (WRITE 엔드포인트에만) — UUID v4 자동 부착
     - 인증 헤더: `Authorization: Bearer <token>` (T-912 후)
     - 에러 매핑: 백엔드 에러코드 → FE 예외 클래스
   - 타입: 생성된 `schema.ts`를 import하되 구체 클래스는 피함 (인터페이스/타입 only).
   - 예시: `const response = await apiClient.get<GetBookingResponse>('/bookings/{id}', { id })`

5. **스크립트 호출 타이밍**
   - 백엔드 OpenAPI 변경 후: `npm run sync:api && npm run gen:api`
   - 개발 워크플로우: `.git/hooks/post-merge`에 자동 실행 (선택사항)
   - CI: 모든 PR에서 타입 drift 검증.

## Acceptance Criteria
- [ ] `npm run gen:api` 성공 → `src/lib/api/schema.ts` 생성 (>100줄, 주요 경로 포함)
- [ ] `npm run sync:api && npm run gen:api` 체인 실행 가능 (오류 없음)
- [ ] 생성된 schema.ts에 최소 10개 경로 타입 포함 (list, create, read, update, cancel 등)
- [ ] validator 통과 (drift 감지 없음)
- [ ] `npm run typecheck` 오류 없음 (schema.ts import 후)

## Verification
```bash
./scripts/verify-task.sh T-911
# 자동 검증:
# 1. npm run gen:api 성공 여부
# 2. src/lib/api/schema.ts 생성 여부
# 3. 타입 drift 검증
# 4. npm run typecheck 통과 여부
```

## Rollback
```bash
# schema.ts 롤백 (버전 관리)
git checkout HEAD -- src/lib/api/schema.ts

# 또는 재생성
npm run gen:api
```

## Notes
- **의존성:** T-912(AuthProvider), T-008~T-027(API 호출 태스크) 전부 이 타입 필요. Blocks 체인 최상위.
- **재사용:** 생성된 타입은 컴포넌트, 상태 관리, 테스트에서 import 사용 (절대 수동 타입 정의 금지).
- **OpenAPI 실패:** 백엔드 openapi.yaml 문법 오류 시 → 백엔드 PR에 리뷰/협의 필요.
- **Next.js 조기 실패:** 타입 생성 전 빌드하면 import 실패. 빌드 전 `npm run gen:api` 필수 (setup.sh에 포함).
