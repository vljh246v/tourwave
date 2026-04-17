# Core Beliefs — 팀 핵심 원칙

## 아키텍처 원칙

### 1. 헥사고날 아키텍처 경계는 절대 역방향으로 넘지 않는다

의존 방향: `adapter.in → application → domain` / `adapter.out → port`

- `domain`에 Spring/JPA/Web import 금지
- `application`에서 `adapter.out` 구체 클래스 import 금지

**이유**: 경계 위반은 테스트 불가능한 코드를 만들고 결합도를 높임

### 2. 트랜잭션 경계는 application 레이어에서 관리한다

- 좌석/정원 흐름은 occurrence 행 락을 먼저 획득
- `adapter.in` 또는 `domain`에서 `@Transactional` 사용 금지

**이유**: 트랜잭션 경계가 흩어지면 락 순서 버그와 데이터 정합성 오류로 이어짐

### 3. 도메인 규칙은 domain 레이어에만 있다

- 예약 상태 머신, 불변식(`confirmedSeats + offeredSeats <= capacity`)은 domain 엔티티에 위치
- application은 유스케이스 조합만, adapter는 변환만

**이유**: 비즈니스 로직이 분산되면 일관성 유지가 불가능

## 운영 원칙

### 4. 모든 상태 변경 엔드포인트에 Idempotency-Key 필수

예약 create/approve/reject/cancel, offer accept/decline, 문의 create/message/close

**이유**: 네트워크 재시도로 인한 중복 실행을 막기 위한 핵심 계약

### 5. 모든 타임스탬프는 UTC

`*_at_utc` 컬럼 사용. DB 타임존 함수 사용 금지, application 레이어에서만 변환

## 하네스 원칙

### 6. 리포지터리 = 단일 진실 원천

설계 결정, 팀 원칙, 도메인 규칙은 반드시 이 리포지터리에 문서화

**이유**: 에이전트는 리포지터리 밖 정보에 접근 불가

### 7. 하네스 진화 원칙

에이전트가 반복 실수를 하면 에이전트 문제가 아니라 하네스 문제

실패 → `agent-failures.md` 기록 → 2회 시 `golden-principles.md` 승격 → 3회 시 구조적 강제
