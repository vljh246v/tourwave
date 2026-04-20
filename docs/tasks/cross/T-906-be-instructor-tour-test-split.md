---
id: T-906
title: "T-906 — [BE] InstructorAndTourControllerIntegrationTest 분리 (테스트 독립성)"
aliases: [T-906]

repo: tourwave
area: be
milestone: cross
domain: tour
layer: test
size: M
status: done

depends_on: []
blocks: []
sub_tasks: []

github_issue: 10
exec_plan: ""

created: 2026-04-18
updated: 2026-04-20
---

#status/done #area/be

# T-906 — [BE] InstructorAndTourControllerIntegrationTest 분리 (테스트 독립성)

## 파일 소유권
WRITE:
  - `src/test/kotlin/com/demo/tourwave/adapter/in/web/instructor/InstructorControllerIntegrationTest.kt` (신규)
  - `src/test/kotlin/com/demo/tourwave/adapter/in/web/tour/TourControllerIntegrationTest.kt` (신규)

DELETE:
  - `src/test/kotlin/com/demo/tourwave/adapter/in/web/InstructorAndTourControllerIntegrationTest.kt` (또는 다른 위치)

READ:
  - `src/test/kotlin/com/demo/tourwave/adapter/in/web/InstructorAndTourControllerIntegrationTest.kt` (현 상태)
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/instructor/InstructorProfileController.kt`
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/instructor/InstructorRegistrationController.kt`
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/tour/TourCommandController.kt`

DO NOT TOUCH:
  - 다른 컨트롤러 테스트 (격리 유지)

## SSOT 근거
- 감사 관찰 `BE-instructor.md` — "통합 테스트 분리 필요: InstructorAndTourControllerIntegrationTest → InstructorControllerIntegrationTest + TourControllerIntegrationTest 분리 권장"
- 테스트 규율 (CLAUDE.md) — 각 컨트롤러 독립 테스트 권장

## 현재 상태 (갭)
- [ ] 단일 test 클래스에 instructor + tour 엔드포인트 혼재
- [ ] 테스트 격리 부족: instructor 테스트 실패 시 tour 조회 불가 또는 vice versa
- [ ] 독립 실행 및 debugging 어려움

## 구현 지침
1. **기존 InstructorAndTourControllerIntegrationTest 분석**:
   - instructor 테스트 케이스 식별 (profile CRUD, registration flow)
   - tour 테스트 케이스 식별 (create, update, publish, archive)
2. **InstructorControllerIntegrationTest 신규 생성**:
   - InstructorProfileController: getMyProfile, createMyProfile, updateMyProfile, getPublicProfile
   - InstructorRegistrationController: submitRegistration, approveRegistration, rejectRegistration
   - 기존 테스트 케이스 이동 + 권한 검증 추가
3. **TourControllerIntegrationTest 신규 생성**:
   - TourCommandController (또는 TourController): create, update, publish, archive, listByOrganization
   - 기존 테스트 케이스 이동
4. **기존 파일 삭제**: InstructorAndTourControllerIntegrationTest (또는 정확한 파일명)

## Acceptance Criteria
- [ ] InstructorControllerIntegrationTest 신규 생성 (instructor 엔드포인트 모두 커버)
- [ ] TourControllerIntegrationTest 신규 생성 (tour 엔드포인트 모두 커버)
- [ ] 기존 InstructorAndTourControllerIntegrationTest 삭제
- [ ] `./gradlew test --tests "*.InstructorControllerIntegrationTest"` 통과
- [ ] `./gradlew test --tests "*.TourControllerIntegrationTest"` 통과
- [ ] 독립 실행 시 서로 부작용 없음

## Verification
`./scripts/verify-task.sh T-906`
- InstructorControllerIntegrationTest 단독 실행
- TourControllerIntegrationTest 단독 실행
- 동시 실행 (parallel test)

## Rollback
`git checkout -- src/test/kotlin/com/demo/tourwave/adapter/in/web/InstructorAndTourControllerIntegrationTest.kt`

## Notes
- 기존 파일 위치 확인 필수 (정확한 경로 agent 조사)
- tour 테스트가 instructor와 의존 관계가 있으면 setup 절차 분리 (예: tour 생성 시 instructor 할당이 선행)
- 각 테스트 클래스에 @TestMethodOrder로 순서 보장 고려 (하지만 독립성 우선)
