# Next Development Backlog

이 문서는 Sprint 6 이후 기준으로 "다음에 무엇을 개발할지"를 handoff 용도로 정리한다.

## 1. Priority Order

### P0

- OpenAPI와 실제 구현 동기화
- auth/account/me 실제 플로우 도입
- organization/member management 최소 CRUD
- real MySQL container 기반 CI 검증

### P1

- assets upload / complete / attachment flow
- public review aggregation by tour / instructor
- calendar export
- worker distributed lock / observability

### P2

- announcements / favorites / reports
- payment webhook / callback
- Gradle 멀티모듈 분리

## 2. Recommended Next Sprint Themes

### Theme A. Contract And Auth Hardening

- `04_openapi.yaml` 정리
- `03_api_catalog.md` 정리
- auth / JWT / me endpoints 구현
- integration test 보강

### Theme B. Operator Product Surface

- organization/member APIs
- instructor registration/profile APIs
- tour/occurrence authoring APIs

### Theme C. Platform Infra Hardening

- real MySQL container tests
- distributed lock
- worker execution metrics

## 3. Immediate Actionable Tasks

1. `13_api_status_matrix.md`를 기준으로 OpenAPI drift를 먼저 정리한다.
2. auth/account/me를 product 수준으로 올린다.
3. worker 운영 안정성 항목을 넣는다.
4. 남은 공용 테스트 시나리오를 `07_test_scenarios.md`와 맞춰 보강한다.

## 4. Rules For Future Tickets

- 상위 티켓을 만들기 전에 구현 surface를 `13_api_status_matrix.md`와 대조한다.
- 서브티켓은 `application`, `adapter`, `test` 축으로 쪼갠다.
- 기능 완료 후에는 테스트 추가와 `./gradlew test` 확인을 함께 완료 기준으로 둔다.
