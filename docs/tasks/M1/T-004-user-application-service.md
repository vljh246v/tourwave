---
id: T-004
title: "T-004 — [BE] user — User ApplicationService 도입 (계층 격리)"
aliases: [T-004]

repo: tourwave
area: be
milestone: M1
domain: auth
layer: application
size: M
status: backlog

depends_on: []
blocks: ['T-005', 'T-006']
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-18
updated: 2026-04-18
---

#status/backlog #area/be

# T-004 — [BE] user — User ApplicationService 도입 (계층 격리)

## 파일 소유권
WRITE:
  - `src/main/kotlin/com/demo/tourwave/application/user/UserService.kt` (신규 — application service)
  - `src/main/kotlin/com/demo/tourwave/application/user/UserPort.kt` (Port 인터페이스 정의)
  - `src/main/kotlin/com/demo/tourwave/bootstrap/UserConfig.kt` (빈 설정)
  - `src/test/kotlin/com/demo/tourwave/application/user/UserServiceTest.kt` (단위 테스트)

READ:
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/auth/MeController.kt` (현재 호출 지점)
  - `src/main/kotlin/com/demo/tourwave/application/auth/AuthCommandService.kt` (auth 도메인 서비스)
  - `src/main/kotlin/com/demo/tourwave/domain/user/User.kt` (도메인 엔티티)
  - `docs/audit/BE-user.md` §관찰된 문제 #1, #6

DO NOT TOUCH:
  - `src/main/kotlin/com/demo/tourwave/domain/user/` (도메인 로직 변경 금지)
  - `src/main/kotlin/com/demo/tourwave/adapter/out/persistence/user/JpaUserRepositoryAdapter.kt`

## SSOT 근거
- `docs/audit/BE-user.md` §관찰된 문제 #1: "ApplicationService 부재 — user 도메인에 전용 ApplicationService 없음. 프로필 관리는 auth 도메인의 MeService에서만 처리"
- `docs/audit/BE-user.md` §관찰된 문제 #6: "MeService와 auth 강결합 — MeService.updateCurrentUser()가 auth 도메인의 validation 함수(requireValidDisplayName) 직접 호출"
- `docs/architecture.md` §아키텍처: "application 레이어가 port 인터페이스에만 의존"

## 현재 상태 (갭)
- [ ] user 도메인에 전용 ApplicationService 없음
- [ ] 프로필 관리 로직이 auth 도메인의 MeService에 분산
- [ ] MeService가 auth 도메인 validation 함수(requireValidDisplayName) 직접 호출 → 계층 위반
- [ ] user 도메인의 비즈니스 로직(상태 전이, 검증)을 담당할 서비스 부재

## 구현 지침
1. `UserPort` 인터페이스 정의 (메서드: `findById(id)`, `findByEmail(email)`, `save(user)`, `deleteById(id)`)
2. `UserService` 생성 (의존성: `UserPort`, `AuditEventPort`)
   - `getCurrentUser(userId): User`
   - `updateProfile(userId, displayName): User`
   - `getOrCreate(email, displayName): User` (향후 확장용)
3. MeController → UserService로 위임 (auth 도메인 의존성 제거)
4. displayName 검증을 UserService 내부로 이동 (auth 도메인 호출 제거)
5. user 상태 변경 시 감사 이벤트 기록 (AuditEventPort.append)
6. FakeUserPort 구현 (테스트용, support/FakeRepositories.kt에 추가)

## Acceptance Criteria
- [ ] `UserPort` 인터페이스 정의 완료
- [ ] `UserService` 구현 완료 (4개 메서드 이상)
- [ ] MeController 수정 완료 (UserService 호출로 변경)
- [ ] displayName 검증이 UserService 내부에서 일어남 (auth 의존성 제거)
- [ ] FakeUserPort 구현 완료
- [ ] `./gradlew test --tests "*UserServiceTest"` 통과
- [ ] `./gradlew test --tests "*MeControllerIntegrationTest"` 통과 (기존 테스트 회귀)

## Verification
`./scripts/verify-task.sh T-004`
예상 결과: build ✓ / test ✓ / lint ✓ / layers ✓ / docs ✓

## Rollback
```bash
git checkout -- src/main/kotlin/com/demo/tourwave/application/user/
git checkout -- src/main/kotlin/com/demo/tourwave/bootstrap/UserConfig.kt
git checkout -- src/main/kotlin/com/demo/tourwave/adapter/in/web/auth/MeController.kt
git clean -fd src/test/kotlin/com/demo/tourwave/application/user/
./gradlew clean test
```

## Notes
- `UserPort`는 persistence port (JPA 어댑터로 구현됨)
- displayName 검증: 비어있지 않음, 255자 이하, 특수문자 필터 (auth 도메인 validation 함수 복제 or 공유 util로 이동)
- T-005에서 UserStatus enum 완성 후, UserService.updateStatus() 메서드 추가 예정
- T-006 프로필 테스트에서 UserService 호출 패턴 검증
