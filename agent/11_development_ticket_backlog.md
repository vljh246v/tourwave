# Tourwave 개발 티켓 백로그

## 1. 개발 시작 우선순위

- 현재는 `booking` 핵심 command 흐름이 가장 많이 구현되어 있다.
- 다음 개발은 `participant / invitation / attendance` 축부터 시작하는 것이 맞다.
- 이유는 이후 `review`, `refund`, `export`, `manual operations`가 모두 이 모델을 기반으로 하기 때문이다.
- 따라서 전체 우선순위는 다음 순서를 권장한다.
  1. 도메인 완성: participant / invitation / attendance
  2. 조회 및 운영 기능: booking detail / inquiry 개선 / waitlist 운영
  3. 환불 및 취소 정책 엔진
  4. 인증 / 권한 / 조직 모델
  5. 운영정책 배치 및 감사로그 확장
  6. DB 전환 및 실서비스 인프라

## 2. Phase 1: Participant / Attendance 기반 구축

### TW-001 Participant 도메인 모델 추가

- 목표
  - booking leader 외에 실제 참가자 개념을 시스템에 도입한다.
- 배경
  - 현재는 booking 단위만 있고 participant 개별 상태가 없다.
  - 이후 invitation, attendance, review eligibility 구현의 선행조건이다.
- 주요 작업
  - `Participant` 엔티티 추가
  - 필드 정의: `participantId`, `bookingId`, `userId`, `status`, `invitedAt`, `respondedAt`
  - participant 상태 enum 정의
  - repository port 및 in-memory adapter 추가
  - fixture 및 테스트 유틸 추가
- 완료 조건
  - 하나의 booking에 여러 participant 연결 가능
  - participant 저장/조회 가능
  - 기본 단위 테스트 통과

### TW-002 Booking-Participant 관계 규칙 정의

- 목표
  - leader와 participant의 관계 및 책임 경계를 명확히 한다.
- 배경
  - 현재 booking의 `partySize`만 존재하고 실제 인원 엔티티 연결이 없다.
- 주요 작업
  - leader 자동 participant 포함 여부 결정
  - `partySize`와 participant 수 동기화 규칙 정의
  - booking 취소 시 participant 상태 cascade 규칙 정의
  - 상태표를 문서와 코드 테스트에 동시에 반영
- 완료 조건
  - booking-participant 관계 규칙 문서화
  - partySize와 participant 수 정합성 테스트 통과

### TW-003 Invitation 생성 API

- 목표
  - booking leader가 동행인을 초대할 수 있게 한다.
- 배경
  - 문서상 participant invitation 흐름이 핵심인데 아직 전혀 없다.
- 주요 작업
  - `POST /bookings/{bookingId}/participants/invitations` 설계
  - 초대 대상 식별 방식 결정
  - 권한 검증 추가
  - 중복 초대 차단
  - terminal booking 차단
  - audit 기록 추가
- 완료 조건
  - 초대 생성 성공 케이스 통과
  - 중복 초대/권한 없음/terminal booking 테스트 통과

### TW-004 Invitation 수락/거절 API

- 목표
  - participant가 초대를 수락 또는 거절할 수 있게 한다.
- 배경
  - invitation 생성만 있고 응답이 없으면 흐름이 완성되지 않는다.
- 주요 작업
  - accept command 추가
  - decline command 추가
  - 응답 가능 상태 검증
  - 이미 응답한 invitation 재처리 차단
  - 응답 시각 기록
  - audit 기록 추가
- 완료 조건
  - 수락/거절 상태 전이 테스트 통과
  - 중복 응답 차단 테스트 통과

### TW-005 Invitation 만료 규칙

- 목표
  - 문서에 정의된 48시간 만료 및 시작 6시간 전 차단 규칙을 구현한다.
- 배경
  - 현재는 invitation 기능 자체가 없고 시간 경계 규칙도 반영되지 않았다.
- 주요 작업
  - invitation expiry 계산 로직 추가
  - `Clock` 기반 시간 비교 구현
  - 48시간 만료 규칙 반영
  - 시작 6시간 전 응답 차단 규칙 반영
  - lazy expiration 또는 batch expiration 전략 결정
- 완료 조건
  - 48시간 경계 테스트 통과
  - 6시간 경계 테스트 통과
  - 만료 후 응답 차단 테스트 통과

### TW-006 Attendance 기록 모델

- 목표
  - participant 단위 출석 기록을 도입한다.
- 배경
  - 현재 review eligibility가 `booking completed`에 묶여 있어 도메인 정밀도가 낮다.
- 주요 작업
  - attendance 상태 enum 정리
  - participant별 check-in / no-show / attended 기록 모델 추가
  - attendance 기록 command 설계
  - 운영자 권한 검증 추가
  - attendance audit 추가
- 완료 조건
  - 운영자가 participant attendance 기록 가능
  - participant별 attendance 조회 가능
  - 출석 상태 테스트 통과

### TW-007 Review 자격 규칙 개편

- 목표
  - review 작성 자격을 participant attendance 기준으로 바꾼다.
- 배경
  - 현재는 `COMPLETED booking의 leader` 기준이라 문서 정의보다 단순하다.
- 주요 작업
  - review eligibility 로직 수정
  - participant attendance 기반 검증으로 변경
  - leader 특례 여부 명확화
  - 기존 review 테스트 개편
- 완료 조건
  - 참석자만 리뷰 작성 가능
  - 미참석자 리뷰 차단
  - 중복 리뷰 방지 유지

## 3. Phase 2: 조회 및 운영 기능 보강

### TW-008 Booking 상세 조회 API

- 목표
  - command-only 구조를 보완하고 실제 운영에 필요한 예약 상세 조회를 제공한다.
- 주요 작업
  - booking detail query service 추가
  - participant, offer, attendance 포함 DTO 설계
  - 접근 권한 검증 추가
- 완료 조건
  - booking 단건 상세 조회 가능
  - leader / org operator 기준 접근 테스트 통과

### TW-009 Inquiry 초기 메시지 저장 보완

- 목표
  - inquiry 생성 시 전달받은 첫 메시지를 실제로 저장한다.
- 주요 작업
  - inquiry create flow 수정
  - 최초 message 자동 생성
  - message author 정보 연결
- 완료 조건
  - inquiry 생성 직후 message 1건 존재
  - 기존 메시지 흐름과 충돌 없음

### TW-010 Inquiry 단건/목록 조회 개선

- 목표
  - inquiry 조회 기능을 실제 사용 가능한 수준으로 끌어올린다.
- 주요 작업
  - inquiry detail API 추가
  - inquiry list API 추가
  - cursor pagination 실제 구현
  - access policy 재검증
- 완료 조건
  - cursor/limit 동작 테스트 통과
  - 단건/목록 조회 동작 확인

### TW-011 Waitlist 운영자 수동 제어

- 목표
  - 자동 승격 외에 운영자 수동 개입 기능을 제공한다.
- 주요 작업
  - force offer command
  - skip command
  - admin note 필드 추가
  - skip count 기록
- 완료 조건
  - 수동 승격/스킵 동작
  - audit에 운영 이력 남음

### TW-012 Participant 명단 조회 / Export

- 목표
  - 운영자가 참가자 명단을 조회하고 내보낼 수 있게 한다.
- 주요 작업
  - occurrence 기준 roster query
  - JSON 또는 CSV export API 추가
  - participant 상태/attendance 포함
- 완료 조건
  - roster 조회 가능
  - export 결과 검증 테스트 통과

## 4. Phase 3: 환불 및 취소 정책 엔진

### TW-013 Refund Policy Calculator

- 목표
  - 환불 규칙을 분리된 정책 계산기로 구현한다.
- 주요 작업
  - 48시간 경계 계산
  - occurrence cancel / leader cancel / reject / offer expire 케이스 정의
  - `FULL_REFUND`, `NO_REFUND`, `REFUND_PENDING` 판단 모델 설계
- 완료 조건
  - 정책 단위 테스트로 케이스별 계산 검증

### TW-014 Refund Preview API

- 목표
  - 취소 전에 환불 결과를 미리 확인할 수 있게 한다.
- 주요 작업
  - refund preview endpoint 설계
  - 예상 refund type, reason code 응답
  - 취소 가능 여부 포함
- 완료 조건
  - 경계시간별 preview 테스트 통과

### TW-015 Booking 취소 로직에 환불 정책 연결

- 목표
  - 실제 cancel command가 환불 정책을 사용하도록 변경한다.
- 주요 작업
  - cancel flow 리팩터링
  - policy calculator 연결
  - booking/refund 상태 전이 정리
- 완료 조건
  - 취소 시 refund 상태 일관성 보장
  - 기존 취소 테스트와 신규 환불 테스트 통과

### TW-016 Payment / Refund 상태 모델 도입

- 목표
  - 향후 gateway 연동을 위한 최소 결제 상태 저장 구조를 만든다.
- 주요 작업
  - payment 또는 transaction record 모델 설계
  - refund request id 및 상태 저장
  - booking과 payment 연계
- 완료 조건
  - booking별 결제/환불 상태 추적 가능

### TW-017 Refund 실패 보상 시나리오

- 목표
  - 환불 실패 시 재시도와 운영자 검토가 가능한 구조를 만든다.
- 주요 작업
  - retryable failure 상태 정의
  - operator review queue 또는 상태값 정의
  - 에러코드 분리
- 완료 조건
  - API 실패 mock 기준 compensation 테스트 통과

## 5. Phase 4: 인증 / 권한 / 조직 모델

### TW-018 실제 User persistence 완성

- 목표
  - 현재 `TODO()` 상태인 user persistence를 구현한다.
- 주요 작업
  - user repository 구현
  - `findByEmail`, `findById`, `save` 완성
  - user 테스트 보강
- 완료 조건
  - user adapter `TODO()` 제거
  - 단위 테스트 통과

### TW-019 Role 모델 정식 도입

- 목표
  - 문서상의 역할 모델을 실제 코드에 반영한다.
- 주요 작업
  - `USER`, `INSTRUCTOR`, `ORG_MEMBER`, `ORG_ADMIN`, `ORG_OWNER` 정의
  - 권한 매트릭스 정리
  - API별 접근 제어 정책 반영
- 완료 조건
  - role 기반 접근 테스트 통과

### TW-020 Authz Guard 리팩터링

- 목표
  - 헤더 중심 actor 처리 로직을 공통 authz 계층으로 정리한다.
- 주요 작업
  - actor context resolver 분리
  - permission evaluator 도입
  - controller 중복 제거
- 완료 조건
  - booking/inquiry/review API가 공통 authz 구조 사용

### TW-021 Organization / Instructor / Tour 최소 모델

- 목표
  - 상위 비즈니스 도메인을 occurrence와 연결한다.
- 주요 작업
  - organization, instructor, tour 최소 엔티티 설계
  - occurrence와의 관계 연결
  - query 응답에 상위 식별자 노출
- 완료 조건
  - occurrence가 어느 org/tour/instructor 소속인지 표현 가능

## 6. Phase 5: 운영정책 / 배치 / 감사로그

### TW-022 Offer Expiration 배치 작업

- 목표
  - offered booking의 자동 만료 처리를 배치로 수행한다.
- 주요 작업
  - scheduler 또는 job runner 추가
  - expired offer sweep 구현
  - 만료 후 waitlist 재계산 여부 결정
- 완료 조건
  - 자동 만료 integration test 통과

### TW-023 Invitation Expiration 배치 작업

- 목표
  - invitation 만료를 자동 처리한다.
- 주요 작업
  - pending invitation sweep
  - 만료 상태 전이 및 재응답 차단
- 완료 조건
  - invitation 만료 배치 테스트 통과

### TW-024 Audit Log 확장

- 목표
  - 운영 추적성을 강화한다.
- 주요 작업
  - `before_json`, `after_json`, `reason_code` 구조 추가
  - actor, timestamp, event type 표준화
  - 주요 mutation 이벤트 payload 정리
- 완료 조건
  - 핵심 mutation의 audit payload 검증 테스트 존재

### TW-025 Timezone 정책 반영

- 목표
  - 시간 경계 계산을 회차의 로컬 시간 기준으로 정확하게 수행한다.
- 주요 작업
  - occurrence timezone 저장
  - 48시간/6시간 판단을 timezone-aware 로직으로 변경
  - DST 경계 테스트 추가
- 완료 조건
  - timezone 기준 경계 계산 테스트 통과

## 7. Phase 6: 영속성 및 실서비스 인프라

### TW-026 JPA / Postgres 영속 어댑터 도입

- 목표
  - in-memory 저장소를 실제 영속 저장소로 전환한다.
- 주요 작업
  - booking, occurrence, inquiry, review, participant persistence 순차 전환
  - repository adapter 구현
  - 테스트 환경 DB 구성
- 완료 조건
  - 핵심 integration test가 DB 기반으로 통과

### TW-027 동시성 제어

- 목표
  - capacity/waitlist 관련 race condition을 방지한다.
- 주요 작업
  - optimistic 또는 pessimistic lock 전략 결정
  - idempotency store 영속화
  - 동시 예약 테스트 작성
- 완료 조건
  - 초과 승인 없는 동시성 테스트 통과

### TW-028 운영 배치와 재시도 인프라

- 목표
  - 만료/환불 재시도/TTL purge를 안정적으로 실행할 수 있게 한다.
- 주요 작업
  - job scheduler 정리
  - retry policy 정리
  - 실패 job 모니터링 지점 추가
- 완료 조건
  - 최소 1개 배치와 1개 재시도 흐름이 운영 가능 수준으로 구현

### TW-029 OpenAPI 동기화

- 목표
  - 구현과 문서의 불일치를 줄인다.
- 주요 작업
  - 신규 API를 OpenAPI에 반영
  - gap report 갱신
  - 에러코드 정리
- 완료 조건
  - 현재 구현 기능 기준 spec mismatch 제거

### TW-030 E2E 시나리오 테스트 팩

- 목표
  - 문서 시나리오를 회귀 가능한 테스트 세트로 만든다.
- 주요 작업
  - `07_test_scenarios.md` 시나리오와 테스트 매핑
  - 구현 완료된 기능부터 시나리오 자동화
  - 시나리오 네이밍 표준화
- 완료 조건
  - 문서 시나리오와 테스트 클래스가 추적 가능하게 연결됨

## 8. 추천 실제 착수 순서

- 바로 시작할 1차 묶음
  - `TW-001`
  - `TW-002`
  - `TW-003`
  - `TW-004`
  - `TW-005`
  - `TW-006`
  - `TW-007`
- 이유
  - 이 7개가 끝나야 review, inquiry, refund, export, 운영정책으로 자연스럽게 확장된다.
  - 현재 코드베이스에서 가장 적은 구조 변경으로 가장 큰 도메인 확장을 만들 수 있다.

## 9. 한줄 결론

- 지금은 결제나 인프라보다 먼저 `participant / invitation / attendance`를 완성해야 한다.
- 이 축이 정리되면 나머지 티켓은 도메인적으로 훨씬 명확하게 연결된다.
