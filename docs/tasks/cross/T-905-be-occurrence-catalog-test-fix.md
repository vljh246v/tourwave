---
id: T-905
title: "T-905 — [BE] OccurrenceCatalogControllerIntegrationTest 수정 (main fail)"
aliases: [T-905]

repo: tourwave
area: be
milestone: cross
domain: occurrence
layer: test
size: M
status: done

depends_on: []
blocks: []
sub_tasks: []

github_issue: 9
exec_plan: ""

created: 2026-04-18
updated: 2026-04-20
---

#status/done #area/be

# T-905 — [BE] OccurrenceCatalogControllerIntegrationTest 수정 (main fail)

## 파일 소유권
WRITE:
  - `src/test/kotlin/com/demo/tourwave/adapter/in/web/occurrence/OccurrenceCatalogControllerIntegrationTest.kt` (수정)

READ:
  - `src/test/kotlin/com/demo/tourwave/adapter/in/web/occurrence/OccurrenceCatalogControllerIntegrationTest.kt` (현 상태)
  - `src/main/kotlin/com/demo/tourwave/adapter/in/web/occurrence/OccurrenceCatalogController.kt`
  - `src/main/kotlin/com/demo/tourwave/application/occurrence/OccurrenceCatalogService.kt`

DO NOT TOUCH:
  - 다른 occurrence 테스트 (부분 격리)
  - T-902, T-906 파일

## SSOT 근거
- `CLAUDE.md` — "알려진 커버리지 미비 항목: OccurrenceCatalogControllerIntegrationTest — main 브랜치에서 기존부터 실패"
- 감사 관찰: `BE-occurrence.md` (조사 필요)

## 현재 상태 (갭)
- [ ] main 브랜치에서 test 실패 (기존 이슈, 이 브랜치 원인 아님)
- [ ] 실패 원인 미파악 (catalog vs. 상세 조회, 데이터 상태, 필터 로직 중 하나)
- [ ] 테스트 환경 격리 또는 setup 문제 가능성

## 구현 지침
1. 실패 원인 파악:
   - `./gradlew test --tests "*.OccurrenceCatalogControllerIntegrationTest"` 실행
   - exception 스택트레이스 분석 (AssertionError, validation error, 404 등)
   - 실패하는 특정 test 메서드 식별
2. 가능한 원인별 수정:
   - **데이터 설정**: setUp() occurrence 데이터 생성 순서/상태 검증
   - **필터 로직**: list 조회 시 visibility/date range 필터 회귀 가능성
   - **권한**: public vs. operator 엔드포인트 권한 검증 오류
   - **테스트 격리**: 다른 occurrence 테스트의 leftover 데이터
3. develop과 main 모두 검증

## Acceptance Criteria
- [ ] `./gradlew test --tests "*.OccurrenceCatalogControllerIntegrationTest"` develop 통과
- [ ] `./gradlew test --tests "*.OccurrenceCatalogControllerIntegrationTest"` main 통과
- [ ] 테스트 격리 확인 (다른 테스트 부작용 없음)

## Verification
`./scripts/verify-task.sh T-905`
- develop + main 양쪽에서 테스트 실행
- 스택트레이스 로깅 확인

## Rollback
`git checkout src/test/kotlin/com/demo/tourwave/adapter/in/web/occurrence/OccurrenceCatalogControllerIntegrationTest.kt`

## Notes
- 실패가 "main에서만"이면 feature branch → main merge 시 conflict resolution 또는 base 코드 변경 추정
- catalog는 public read이므로 권한이 원인일 가능성 낮음. 데이터 setup 또는 필터 로직 의심
