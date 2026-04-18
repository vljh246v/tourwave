# T-902 — [BE] CommunicationReportingIntegrationTest 수정 (main fail)

## Meta
- ID: T-902
- Milestone: Cross-cutting
- Domain: common
- Area: BE
- Layer: test
- Size: M
- Depends on: 없음
- Blocks: 없음
- GitHub Issue: #6
- Status: Backlog

## 파일 소유권
WRITE:
  - `src/test/kotlin/com/demo/tourwave/adapter/in/web/CommunicationReportingIntegrationTest.kt` (수정)

READ:
  - `src/test/kotlin/com/demo/tourwave/adapter/in/web/CommunicationReportingIntegrationTest.kt` (현 상태 분석)
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/announcement/AnnouncementController.kt`
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/inquiry/InquiryController.kt`

DO NOT TOUCH:
  - 다른 통합 테스트 (T-905, T-906 담당)

## SSOT 근거
- `CLAUDE.md` — "알려진 커버리지 미비 항목: CommunicationReportingIntegrationTest 미존재 — main 브랜치에서 기존부터 실패"
- 감사 관찰 `BE-announcement.md` — develop에서 성공, main에서 실패 (merged state 차이)

## 현재 상태 (갭)
- [ ] main 브랜치에서 test 실패 (develop에서는 성공)
- [ ] 실패 원인: announcement/inquiry 리포지토리 초기화 또는 권한 가드 회귀 추정
- [ ] setUp() 또는 tearDown() 로직 불일치 가능성

## 구현 지침
1. 테스트 실행하여 실패 원인 파악:
   - `./gradlew test --tests "*.CommunicationReportingIntegrationTest"` (develop, main 환경 모두 확인)
   - exception 스택트레이스 분석 (AssertionError, NotFoundException, 403 Forbidden 등)
2. 가능한 원인별 수정:
   - **리포지토리 미초기화**: setUp() 또는 @BeforeEach에서 clear() + save() 순서 검증
   - **권한 가드 회귀**: OrganizationAccessGuard 호출 누락 또는 역할 검증 로직 오류
   - **데이터 상태**: 통합 테스트 컨테이너의 isolation 확인 (다른 테스트의 leftovers)
3. 수정 후 develop과 main 모두에서 성공 확인

## Acceptance Criteria
- [ ] `./gradlew test --tests "*.CommunicationReportingIntegrationTest"` develop 통과
- [ ] `./gradlew test --tests "*.CommunicationReportingIntegrationTest"` main 통과
- [ ] 테스트 격리 이슈 해결 (다른 테스트 부작용 없음)

## Verification
`./scripts/verify-task.sh T-902`
- develop + main 양쪽 통합 테스트 실행
- 리포지토리 상태 검증 (isEmpty() 체크)

## Rollback
`git checkout src/test/kotlin/com/demo/tourwave/adapter/in/web/CommunicationReportingIntegrationTest.kt`

## Notes
- main 브랜치의 merged state가 중요. feature branch와의 diff 비교 권장 (git diff main develop -- CommunicationReportingIntegrationTest.kt)
- 실패 패턴이 "권한"이면 T-903과 연계 가능 (Idempotency-Key 미구현과 무관, 하지만 권한 검증은 같은 영역)
