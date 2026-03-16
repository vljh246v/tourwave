# Hexagonal Architecture Guardrails (MVP)

이 문서는 Tourwave 구현 시 **Hexagonal Architecture**를 강제하기 위한 기준을 정의한다.

---

## 1) 레이어 정의

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

## 2) 의존 방향 규칙

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

## 3) 패키지 가이드

권장 구조:

- `com.demo.tourwave.domain.<bounded_context>`
- `com.demo.tourwave.application.<bounded_context>`
- `com.demo.tourwave.adapter.in.web.<bounded_context>`
- `com.demo.tourwave.adapter.out.persistence.<bounded_context>`
- `com.demo.tourwave.bootstrap`

---

## 4) 구현 규칙

- Controller는 비즈니스 규칙을 직접 수행하지 않는다.
- UseCase/Service는 Port 인터페이스만 의존한다.
- Repository/JPA 구현은 `adapter.out`에만 위치한다.
- 에러코드 매핑은 `adapter.in`(API 경계)에서 일관되게 처리한다.
- 트랜잭션 경계는 `application`에서 관리한다.

---

## 5) 테스트 규칙

- `domain` 테스트: 상태전이/불변식 중심 단위 테스트
- `application` 테스트: Port mock 기반 유스케이스 테스트
- `adapter.in` 테스트: 계약/에러코드/권한 테스트
- `adapter.out` 테스트: 저장소 매핑/쿼리 테스트

---

## 6) 기존 코드 리팩터 정책 (중요)

현재 코드가 초기 단계에서 구조가 섞여 있을 수 있으므로, 기능 구현 완료 후 아래 순서로 정리한다.

1. 도메인 모델에서 framework 의존 제거
2. 유스케이스를 `application` 계층으로 이동
3. 저장소/외부 연동 구현을 `adapter.out`으로 이동
4. Controller/DTO 매핑을 `adapter.in`으로 고정
5. 패키지/클래스 네이밍을 `agent/09_spec_index.md`와 일치화

리팩터 중에도 아래는 유지해야 한다:

- API 계약(`04_openapi.yaml`) 호환
- 에러코드 정책(`08_operational_policy_tables.md`) 호환
- 테스트 그린 상태 유지

---

## 7) PR 체크리스트

- [ ] domain 계층에 Spring/JPA/Web import 없음
- [ ] application 계층이 adapter concrete 구현에 직접 의존하지 않음
- [ ] write usecase는 idempotency/409/422 정책 준수
- [ ] 패키지 구조가 hexagonal 규칙을 위반하지 않음
- [ ] 리팩터 후 테스트 통과

