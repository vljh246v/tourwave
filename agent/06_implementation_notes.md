# Implementation Notes

이 문서는 "지금 이 저장소에서 개발할 때 바로 필요한 실행 규칙"만 정리한다.

## 1. Architecture Rules

- `10_architecture_hexagonal.md`를 강제 규칙으로 본다.
- `domain`은 Spring/JPA/Web 의존을 갖지 않는다.
- `application`은 port interface만 의존한다.
- `adapter.in`은 DTO 변환과 HTTP 경계만 담당한다.
- `adapter.out`은 DB/외부 시스템 구현만 담당한다.

## 2. Current Runtime Rules

- API 부트스트랩: `com.demo.tourwave.TourwaveApplication`
- Worker 부트스트랩: `com.demo.tourwaveworker.WorkerApplication`
- JPA/Flyway는 `MysqlPersistenceConfig`에서 수동으로 조립된다.
- `mysql`, `mysql-test` 프로필에서는 JPA adapter가 활성화된다.
- 그 외 프로필에서는 in-memory adapter가 활성화된다.

## 3. Persistence Notes

- 런타임 기준 DB는 MySQL이다.
- 테스트 환경의 `mysql-test`는 현재 H2 MySQL compatibility mode를 사용한다.
- 이유는 현재 개발 환경에서 Docker provider를 보장할 수 없기 때문이다.
- 실제 MySQL container 기반 검증이 필요하면 CI나 Docker 가능한 로컬 환경에서 보강한다.

## 4. Transaction And Locking Rules

다음 흐름은 반드시 transaction 안에서 동작해야 한다.

- booking create / approve / reject / cancel / offer accept / offer decline
- waitlist manual promote / skip
- inquiry create / message / close
- participant invitation / accept / decline / attendance
- review create
- refund retry

좌석/정원 관련 흐름은 occurrence lock을 먼저 잡고 처리한다.

## 5. Worker Job Rules

worker job은 도메인 로직을 직접 가지지 않는다.

- job은 `application` service를 호출하는 orchestration layer다.
- offer expiration
- invitation expiration
- refund retry
- idempotency TTL purge

새 job을 추가할 때도 동일 규칙을 지킨다.

## 6. API / Spec Handling Rules

- code-first 구현이 먼저 생기면 `13_api_status_matrix.md`를 먼저 업데이트한다.
- 이후 `03_api_catalog.md`, `04_openapi.yaml`을 맞춘다.
- OpenAPI가 먼저 바뀌었다면 controller와 integration test를 같은 변경 세트에서 맞춘다.

## 7. Testing Rules

- 기능 단위 구현이 끝날 때마다 해당 영역 테스트를 추가한다.
- 마지막에는 항상 `./gradlew test` 전체를 확인한다.
- coverage source of truth는 `14_test_traceability_matrix.md`다.

## 8. Development Guardrails

- domain state transition을 controller에 넣지 않는다.
- repository/JPA mapping은 `adapter.out.persistence`에만 둔다.
- job/scheduler는 `adapter.in.job`에만 둔다.
- 새 예외 코드는 domain/common 에러 코드와 API 응답 매핑을 함께 맞춘다.
- "임시" 문서는 `agent`에 남기지 말고 영구 문서에 흡수하거나 삭제한다.
