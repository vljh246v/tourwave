# Spec Index & Governance

이 문서는 `agent` 폴더를 읽는 다른 에이전트가 어디서부터 무엇을 읽어야 하는지, 어떤 문서가 규범이고 어떤 문서가 현재 상태 요약인지 정의한다.

## 1. Document Groups

### Normative Specs

이 그룹은 구현 기준 문서다.

1. `01_domain_rules.md`
2. `10_architecture_hexagonal.md`
3. `08_operational_policy_tables.md`
4. `05_authz_model.md`
5. `02_schema_mysql.md`
6. `04_openapi.yaml`

### Current-State Handoff Docs

이 그룹은 현재 구현과 운영 상태를 빠르게 이해하기 위한 문서다.

1. `00_overview.md`
2. `11_current_implementation_status.md`
3. `12_runtime_topology_and_operations.md`
4. `13_api_status_matrix.md`
5. `14_test_traceability_matrix.md`
6. `15_next_development_backlog.md`
7. `16_product_delivery_roadmap.md`
8. `17_release_gap_execution_plan.md`

### Supporting Docs

- `03_api_catalog.md`
- `06_implementation_notes.md`
- `07_test_scenarios.md`
- `18_trust_surface_policy.md`
- `19_launch_ops_baseline.md`
- `../docs/launch-readiness-checklist.md`

## 2. Source Of Truth Priority

충돌이 나면 아래 우선순위를 따른다.

1. `01_domain_rules.md`
2. `10_architecture_hexagonal.md`
3. `08_operational_policy_tables.md`
4. `05_authz_model.md`
5. `02_schema_mysql.md`
6. `04_openapi.yaml`
7. `13_api_status_matrix.md`
8. `14_test_traceability_matrix.md`
9. `06_implementation_notes.md`
10. `03_api_catalog.md`
11. `00_overview.md`
12. `11_current_implementation_status.md`
13. `12_runtime_topology_and_operations.md`
14. `15_next_development_backlog.md`
15. `16_product_delivery_roadmap.md`
16. `17_release_gap_execution_plan.md`

규칙:

- 규범 문서가 현재 상태 문서보다 우선한다.
- 단, OpenAPI가 실제 구현보다 뒤처진 경우 현재 구현 확인은 `13_api_status_matrix.md`와 controller/test를 먼저 본다.
- 현재 상태 문서는 규범을 대체하지 않는다. 규범과 구현 차이를 설명하는 역할만 한다.

## 3. Recommended Reading Order

### A. 바로 개발 들어갈 때

1. `00_overview.md`
2. `11_current_implementation_status.md`
3. `12_runtime_topology_and_operations.md`
4. `13_api_status_matrix.md`
5. `16_product_delivery_roadmap.md`
6. `17_release_gap_execution_plan.md`
7. `14_test_traceability_matrix.md`
8. `01_domain_rules.md`
9. `10_architecture_hexagonal.md`
10. `08_operational_policy_tables.md`
11. `15_next_development_backlog.md`

### B. API 계약 수정이 목적일 때

1. `13_api_status_matrix.md`
2. `03_api_catalog.md`
3. `04_openapi.yaml`
4. 관련 controller / integration test

실무 순서:

1. controller / service / integration test로 실제 구현 상태를 확인한다.
2. `13_api_status_matrix.md`를 먼저 current truth에 맞춘다.
3. `04_openapi.yaml`을 target contract 기준으로 정리한다.
4. `03_api_catalog.md`와 관련 handoff 문서를 업데이트한다.

### C. 저장소 / 동시성 / 배치 수정이 목적일 때

1. `12_runtime_topology_and_operations.md`
2. `02_schema_mysql.md`
3. `08_operational_policy_tables.md`
4. `06_implementation_notes.md`
5. 관련 `application`, `adapter.out`, `adapter.in.job` 코드

## 4. Mandatory Working Agreements

- 상태 변경 endpoint는 `Idempotency-Key` 정책을 따라야 한다.
- participant/attendance는 booking 하위가 아니라 participant 단위 규칙을 따른다.
- worker job은 `application` service를 호출하는 orchestration layer여야 한다.
- 테스트 없는 기능 완료 처리는 금지한다.
- `agent` 폴더에는 시점 종속 메모를 남기지 않고, 영구 문서에 흡수하거나 삭제한다.
- 운영 변경은 `launch-readiness-checklist.md`와 actuator/CI 기준까지 같이 맞춘다.
- 이미 완료된 sprint 정리는 `16_product_delivery_roadmap.md`를 보고, 출시까지 남은 gap closure는 `17_release_gap_execution_plan.md`를 기준으로 새 티켓을 만든다.

## 5. Quick Commands

- 전체 테스트: `./gradlew test`
- 핵심 API 회귀: `./gradlew test --tests 'com.demo.tourwave.domain.booking.application.BookingControllerIntegrationTest'`
- MySQL-compatible persistence 회귀: `./gradlew test --tests 'com.demo.tourwave.adapter.out.persistence.jpa.MysqlPersistenceIntegrationTest'`
- OpenAPI contract 회귀: `./gradlew test --tests 'com.demo.tourwave.agent.OpenApiContractVerificationTest'`
