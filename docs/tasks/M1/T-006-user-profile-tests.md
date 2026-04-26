---
id: T-006
title: "T-006 — [BE] user — User 프로필 테스트 강화"
aliases: [T-006]

repo: tourwave
area: be
milestone: M1
domain: auth
layer: domain + application + adapter.in
size: M
status: in-progress

depends_on: ['T-004', 'T-005']
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-18
updated: 2026-04-26
---

#status/in-progress #area/be

# T-006 — [BE] user — User 프로필 테스트 강화

## 파일 소유권
WRITE:
  - `src/test/kotlin/com/demo/tourwave/domain/user/UserTest.kt` (신규 단위 테스트)
  - `src/test/kotlin/com/demo/tourwave/application/user/UserServiceTest.kt` (신규 application 테스트)
  - `src/test/kotlin/com/demo/tourwave/adapter/in/web/user/MeControllerIntegrationTest.kt` (신규 통합 테스트)

READ:
  - `src/main/kotlin/com/demo/tourwave/domain/user/User.kt` (T-005 완료 후)
  - `src/main/kotlin/com/demo/tourwave/application/user/UserService.kt` (T-004 완료 후)
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/auth/MeController.kt` (현재 엔드포인트)
  - `docs/testing.md` (테스트 레이어 규율)

DO NOT TOUCH:
  - `src/main/kotlin/com/demo/tourwave/domain/user/` (domain 로직)
  - `src/main/kotlin/com/demo/tourwave/application/user/` (application 로직)
  - 다른 도메인의 테스트 파일

## SSOT 근거
- `docs/audit/BE-user.md` (전체): user 도메인 테스트 완성도 🟡 (부분 구현)
- `docs/testing.md` §레이어별 테스트 규율: domain은 순수 단위, application은 FakeRepositories 기반, adapter.in은 Spring Context 통합

## 현재 상태 (갭)
- [ ] `UserTest` (domain 순수 단위) 부재
- [ ] `UserStatusTest` (상태 전이) 부재 (T-005에서 별도)
- [ ] `UserService` 통합 테스트 부재 (application 층)
- [ ] `MeController` 통합 테스트 부재 (adapter.in + Spring)
- [ ] 상태 전이 경계 조건 미검증 (ACTIVE → DELETED, SUSPENDED 회귀 불가 등)
- [ ] 프로필 업데이트 감사 이벤트 미검증

## 구현 지침
1. **UserTest** (domain 순수 단위):
   - User 생성 및 기본 속성 검증
   - displayName 유효성 검증 (비어있음, 초과 길이)
   - 상태 전이 규칙 검증 (4개 상태 × 가능한 전이 케이스)
   - 마스킹 로직 검증 (delete 시 email/displayName 변환)

2. **UserServiceTest** (application, FakeRepositories 기반):
   - `getCurrentUser(userId)` — 존재/미존재 케이스
   - `updateProfile(userId, displayName)` — 성공/실패 케이스
   - `suspend(userId)` / `delete(userId)` / `restore(userId)` — 상태 전이 검증
   - 감사 이벤트 기록 검증 (mockAuditEventPort.getCalls())

3. **MeControllerIntegrationTest** (adapter.in, Spring Context + Testcontainers):
   - `GET /me` — 인증된 사용자 조회
   - `PATCH /me` — 프로필 수정 (displayName 변경)
   - `POST /me/deactivate` — 계정 비활성화 (DEACTIVATED 상태 전이)
   - 인증 헤더 미포함 시 401 응답
   - Idempotency-Key 검증 (있으면 좋고, 없어도 ok)

4. 테스트 시나리오:
   - Happy path: 정상 프로필 수정 → 상태 확인
   - Error path: 유효하지 않은 displayName (빈값, 초과) → 400 Bad Request
   - State path: ACTIVE → DEACTIVATED → ACTIVE (복구) / DELETED (영구 삭제)

5. 테스트 도구:
   - JUnit 5, AssertJ (가독성)
   - FakeUserPort 사용 (application 테스트)
   - MockMvc (adapter.in 통합 테스트)

## Acceptance Criteria
- [ ] `UserTest` 작성 완료 (10+ 테스트 케이스)
- [ ] `UserServiceTest` 작성 완료 (8+ 테스트 케이스)
- [ ] `MeControllerIntegrationTest` 작성 완료 (6+ 테스트 케이스)
- [ ] 모든 상태 전이 경로 커버 (4 states × 3+ transitions)
- [ ] 감사 이벤트 기록 검증 포함
- [ ] `./gradlew test --tests "*UserTest"` 통과
- [ ] `./gradlew test --tests "*UserServiceTest"` 통과
- [ ] `./gradlew test --tests "*MeControllerIntegrationTest"` 통과
- [ ] 전체 테스트 `./gradlew test` 통과

## Verification
`./scripts/verify-task.sh T-006`
예상 결과: build ✓ / test ✓ / coverage ✓ / docs ✓

## Rollback
```bash
git clean -fd src/test/kotlin/com/demo/tourwave/domain/user/UserTest.kt
git clean -fd src/test/kotlin/com/demo/tourwave/application/user/UserServiceTest.kt
git clean -fd src/test/kotlin/com/demo/tourwave/adapter/in/web/user/MeControllerIntegrationTest.kt
./gradlew clean test
```

## Notes
- T-004, T-005 완료 후에만 이 태스크 시작 가능
- FakeUserPort는 support/FakeRepositories.kt에 이미 정의되어 있을 것 (또는 신규 생성)
- 상태 전이 매트릭스: ACTIVE → {DEACTIVATED, SUSPENDED, DELETED}, DEACTIVATED → {ACTIVE, DELETED}, SUSPENDED → {ACTIVE, DELETED}, DELETED → {} (terminal)
- 감사 이벤트: USER_PROFILE_UPDATED, USER_SUSPENDED, USER_DELETED, USER_RESTORED
- 향후 coverage 목표: user 도메인 90%+
