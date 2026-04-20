---
id: T-003
title: "T-003 — [BE] common — TimeWindowPolicyService 상수 설정화"
aliases: [T-003]

repo: tourwave
area: be
milestone: M1
domain: infra
layer: application + bootstrap
size: S
status: backlog

depends_on: []
blocks: []
sub_tasks: []

github_issue: null
exec_plan: ""

created: 2026-04-18
updated: 2026-04-18
---

#status/backlog #area/be

# T-003 — [BE] common — TimeWindowPolicyService 상수 설정화

## 파일 소유권
WRITE:
  - `src/main/kotlin/com/demo/tourwave/application/common/TimeWindowPolicyService.kt` (하드코딩 상수 → 주입 인자로 변경)
  - `src/main/resources/application.yml` (timewindow 설정 섹션 추가)
  - `src/main/resources/application-mysql-test.yml` (테스트 프로필 설정)
  - `src/main/kotlin/com/demo/tourwave/bootstrap/CommonConfig.kt` (빈 설정 수정)
  - `src/test/kotlin/com/demo/tourwave/application/common/TimeWindowPolicyServiceTest.kt` (테스트 수정)

READ:
  - `docs/domain-rules.md` (시간 경계 규칙: 6h, 48h, offer 만료)
  - `docs/policies.md` §4.4 Time & Timezone Policy
  - `docs/audit/BE-common.md` §관찰된 문제 #3

DO NOT TOUCH:
  - `src/main/kotlin/com/demo/tourwave/domain/` (도메인 로직)
  - 다른 도메인의 설정 파일들

## SSOT 근거
- `docs/audit/BE-common.md` §관찰된 문제 #3: "TimeWindowPolicyService 상수 — 초대 6시간, 48시간 등의 값이 하드코딩되어 있음 (설정값으로 이동 권장)"
- `docs/domain-rules.md` §Participant Invitations: 초대 6h 차단, 48h 만료 규칙 명시
- `docs/policies.md` §4.4: 타임존 및 시간 정책 중앙화 원칙

## 현재 상태 (갭)
- [ ] `TimeWindowPolicyService` 내 초대 6h, 48h, offer 만료 시간이 하드코딩됨
- [ ] 상수 변경 시 코드 재컴파일 필요
- [ ] 런타임 설정 불가능

## 구현 지침
1. `application.yml`에 `tourwave.timewindow` 섹션 추가:
   ```yaml
   tourwave:
     timewindow:
       invitation-window-minutes: 360  # 6h
       invitation-expiry-hours: 48
       offer-expiry-hours: 24
   ```
2. `TimeWindowPolicyService` 생성자에서 `@ConfigurationProperties` 또는 `@Value` 주입받도록 수정
3. 기존 하드코딩된 Duration/Instant 계산을 설정값 기반으로 변경
4. `CommonConfig`에서 빈 생성 시 설정값 전달
5. 테스트 프로필에서 테스트 친화적인 값으로 오버라이드 (예: 1분, 2분, 3분)
6. 기존 단위 테스트 수정 (설정값 기반 검증)

## Acceptance Criteria
- [ ] `application.yml`에 timewindow 설정 추가 완료
- [ ] `TimeWindowPolicyService` 주입 가능하게 수정 완료
- [ ] 기존 하드코딩 상수 제거 완료
- [ ] `./gradlew test --tests "*TimeWindowPolicyServiceTest"` 통과
- [ ] `./gradlew test` (전체) 통과 (설정 오버라이드 동작 확인)

## Verification
`./scripts/verify-task.sh T-003`
예상 결과: build ✓ / test ✓ / lint ✓ / config ✓ / docs ✓

## Rollback
```bash
git checkout -- src/main/kotlin/com/demo/tourwave/application/common/TimeWindowPolicyService.kt
git checkout -- src/main/kotlin/com/demo/tourwave/bootstrap/CommonConfig.kt
git checkout -- src/main/resources/application.yml
git checkout -- src/test/kotlin/com/demo/tourwave/application/common/TimeWindowPolicyServiceTest.kt
./gradlew clean test
```

## Notes
- `@ConfigurationProperties(prefix = "tourwave.timewindow")` 데이터 클래스 정의 권장
- 테스트에서는 `@SpringBootTest(properties = {...})` 로 오버라이드
- 설정 검증: 초대 윈도우 < 초대 만료 < offer 만료 규칙 확인 권장
- 향후 admin API에서 동적 설정 변경 고려 시 별도 타스크
