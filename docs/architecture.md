# Architecture Guide (Hexagonal + Implementation Rules)

이 문서는 Tourwave의 **Hexagonal Architecture** 강제 기준과 구현 실행 규칙을 정의한다.

---

## Layer Definitions

- `domain`
  - 엔티티, 값 객체, 도메인 규칙, 상태전이만 포함
  - 외부 프레임워크(Spring/JPA/Web) 의존 금지
- `application`
  - 유스케이스 조합, 트랜잭션 경계, 권한/정책 호출 오케스트레이션
  - 인프라 접근은 오직 Port(interface) 경유
- `adapter.in`
  - HTTP Controller, 메시지 consumer 등 입력 어댑터
  - DTO ↔ Command 변환 담당
- `adapter.out`
  - DB/JPA/외부 API 구현체
  - Port 구현 담당
- `bootstrap`
  - Bean wiring, 설정, profile 구성

---

## Dependency Direction Rules

허용:

- `adapter.in -> application`
- `application -> domain`
- `application -> port`
- `adapter.out -> port`

금지:

- `domain -> application/adapter/framework`
- `application -> adapter.out concrete class`
- `adapter.out -> adapter.in`

---

## Package Guide

권장 구조:

- `com.demo.tourwave.domain.<bounded_context>`
- `com.demo.tourwave.application.<bounded_context>`
- `com.demo.tourwave.adapter.in.web.<bounded_context>`
- `com.demo.tourwave.adapter.out.persistence.<bounded_context>`
- `com.demo.tourwave.bootstrap`

---

## Implementation Rules

- Controller는 비즈니스 규칙을 직접 수행하지 않는다. domain state transition을 controller에 넣지 않는다.
- UseCase/Service는 Port 인터페이스만 의존한다.
- Repository/JPA 구현은 `adapter.out.persistence`에만 위치한다.
- job/scheduler는 `adapter.in.job`에만 위치한다.
- 에러코드 매핑은 `adapter.in`(API 경계)에서 일관되게 처리한다. 새 예외 코드는 domain/common 에러 코드와 API 응답 매핑을 함께 맞춘다.
- 트랜잭션 경계는 `application`에서 관리한다.

### Transaction and Locking Rules

다음 흐름은 반드시 transaction 안에서 동작해야 한다.

- booking create / approve / reject / cancel / offer accept / offer decline
- waitlist manual promote / skip
- inquiry create / message / close
- participant invitation / accept / decline / attendance
- review create
- refund retry

좌석/정원 관련 흐름은 occurrence lock을 먼저 잡고 처리한다.

### Worker Job Rules

worker job은 도메인 로직을 직접 가지지 않는다.

- job은 `application` service를 호출하는 orchestration layer다.
- 대상 job: offer expiration, invitation expiration, refund retry, idempotency TTL purge
- 새 job을 추가할 때도 동일 규칙을 지킨다.

---

## Test Discipline

- `domain` 테스트: 상태전이/불변식 중심 단위 테스트
- `application` 테스트: Port mock 기반 유스케이스 테스트
- `adapter.in` 테스트: 계약/에러코드/권한 테스트
- `adapter.out` 테스트: 저장소 매핑/쿼리 테스트

기능 단위 구현이 끝날 때마다 해당 영역 테스트를 추가하고, 마지막에는 `./gradlew test` 전체를 확인한다.
자세한 테스트 시나리오 및 추적 매트릭스는 `docs/testing.md`를 참조한다.

---

## Implementation Notes

### Runtime Bootstrap

- API 부트스트랩: `com.demo.tourwave.TourwaveApplication`
- Worker 부트스트랩: `com.demo.tourwaveworker.WorkerApplication`
- JPA/Flyway는 `MysqlPersistenceConfig`에서 수동으로 조립된다.
- `mysql`, `mysql-test` 프로필에서는 JPA adapter가 활성화된다.
- 그 외 프로필에서는 in-memory adapter가 활성화된다.

### Persistence Notes

- 런타임 기준 DB는 MySQL이다.
- 테스트 환경의 `mysql-test`는 현재 H2 MySQL compatibility mode를 사용한다.
- 실제 MySQL container 기반 검증이 필요하면 CI나 Docker 가능한 로컬 환경에서 보강한다.

### API / Spec Handling

- code-first 구현이 먼저 생기면 API status matrix를 먼저 업데이트한다.
- 이후 `docs/openapi.yaml`을 맞춘다.
- OpenAPI가 먼저 바뀌었다면 controller와 integration test를 같은 변경 세트에서 맞춘다.

---

## Refactor Policy for Legacy Code

현재 코드가 초기 단계에서 구조가 섞여 있을 수 있으므로, 기능 구현 완료 후 아래 순서로 정리한다.

1. 도메인 모델에서 framework 의존 제거
2. 유스케이스를 `application` 계층으로 이동
3. 저장소/외부 연동 구현을 `adapter.out`으로 이동
4. Controller/DTO 매핑을 `adapter.in`으로 고정
5. 패키지/클래스 네이밍을 `README.md`와 일치화

리팩터 중에도 아래는 유지해야 한다:

- API 계약(`docs/openapi.yaml`) 호환
- 에러코드 정책(`docs/policies.md`) 호환
- 테스트 그린 상태 유지

---

## PR Checklist

- [ ] domain 계층에 Spring/JPA/Web import 없음
- [ ] application 계층이 adapter concrete 구현에 직접 의존하지 않음
- [ ] write usecase는 idempotency/409/422 정책 준수
- [ ] 패키지 구조가 hexagonal 규칙을 위반하지 않음
- [ ] 트랜잭션 경계가 `application`에 있음
- [ ] 새 예외 코드에 domain/common 에러 코드와 API 응답 매핑이 함께 포함됨
- [ ] 새 job이 있다면 `application` service 호출 패턴 준수
- [ ] 리팩터 후 테스트 통과 (`./gradlew test`)
- [ ] "임시" 메모는 영구 문서에 흡수하거나 삭제