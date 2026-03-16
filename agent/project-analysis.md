# Tourwave 프로젝트 분석

## 1. 프로젝트 한줄 요약

- Kotlin + Spring Boot 기반의 투어 예약 백엔드 프로토타입이다.
- 현재 구현 중심축은 `booking`, `inquiry`, `review`이며, 영속 계층은 대부분 in-memory adapter로 구성되어 있다.
- 구조는 헥사고널 스타일을 의식하고 있지만, 실제로는 API 계약과 핵심 상태 전이에 집중한 단계다.

## 2. 전반적인 플로우

### 2.1 요청 처리 공통 흐름

1. HTTP 요청이 `adapter/in/web/*Controller`로 들어온다.
2. `RequestHeaderAuthzGuardAdapter`가 `X-Actor-User-Id`, 조직 역할 헤더를 검증한다.
3. Controller가 Web DTO를 Application Command/Query로 변환한다.
4. `application/*Service`가 멱등성(`IdempotencyStore`), 권한, 상태 전이, 비즈니스 규칙을 처리한다.
5. 서비스는 `port` 인터페이스를 통해 Repository/Audit adapter를 호출한다.
6. 결과는 Web DTO로 다시 변환되어 응답된다.
7. 예외는 `GlobalExceptionHandler`에서 `ErrorResponse` 포맷으로 통일된다.

### 2.2 booking 중심 핵심 업무 플로우

1. 사용자가 `POST /occurrences/{occurrenceId}/bookings`로 예약 생성 요청을 보낸다.
2. `BookingCommandService.createBooking`이 `Idempotency-Key`와 요청 해시를 기준으로 중복 요청을 판별한다.
3. `OccurrenceRepository`에서 회차를 조회하거나 기본 회차를 생성한다.
4. 회차가 `CANCELED` 또는 `FINISHED`면 생성이 차단된다.
5. 현재 좌석 수를 계산해 `REQUESTED` 또는 `WAITLISTED` 상태의 `Booking`이 생성된다.
6. 생성 이벤트는 audit에 적재된다.
7. 이후 운영자 또는 사용자 액션으로 승인, 거절, 취소, 완료, 제안 수락/거절, 파티 인원 축소가 가능하다.
8. 좌석이 풀리면 waitlist 승격 로직이 동작한다.

### 2.3 inquiry 플로우

1. 예약 리더가 `POST /occurrences/{occurrenceId}/inquiries`로 문의를 생성한다.
2. booking 소유자와 occurrence 스코프가 맞는지 검증한다.
3. 같은 booking에 기존 inquiry가 있으면 재사용하고, 없으면 생성한다.
4. 사용자/운영자는 `POST /inquiries/{inquiryId}/messages`로 메시지를 남길 수 있다.
5. `InquiryAccessPolicy`가 사용자/조직 운영자 접근 가능 여부를 판단한다.
6. 문의는 `POST /inquiries/{inquiryId}/close`로 종료할 수 있다.
7. 메시지 조회는 `GET /inquiries/{inquiryId}/messages`로 제공된다.

### 2.4 review 플로우

1. 참석 완료 사용자가 투어 후기 또는 강사 후기를 작성한다.
2. `ReviewCommandService`가 평점 범위, 참석 자격, 중복 리뷰 여부를 검증한다.
3. `ReviewRepository`에 저장 후 audit를 남긴다.
4. `GET /occurrences/{occurrenceId}/reviews/summary`에서 회차별 후기 집계를 조회할 수 있다.

## 3. 아키텍처/레이어 특징

### 3.1 bootstrap

- `UseCaseConfig`에서 서비스 빈을 명시적으로 조립한다.
- 프레임워크 의존은 adapter 쪽에 두고, application service는 순수 객체로 유지하려는 의도가 보인다.

### 3.2 adapter/in/web

- REST 엔드포인트는 모두 여기에 모여 있다.
- 컨트롤러는 비교적 얇고, 헤더 기반 인증/인가 컨텍스트를 주입한 뒤 application service에 위임한다.
- 현재 구현 엔드포인트는 총 17개다.
  - booking 10개
  - inquiry 4개
  - review 3개

### 3.3 application

- 실질적인 비즈니스 규칙이 가장 많이 들어 있다.
- 멱등성, 상태 전이, access policy, 감사 로그 적재가 여기서 처리된다.
- `booking`이 가장 완성도가 높고, `review`와 `inquiry`가 그 다음, `user`는 초기 스켈레톤 수준이다.

### 3.4 domain

- `Booking`, `Inquiry`, `Review`, `Occurrence`, `User` 등 핵심 엔티티가 있다.
- `Booking`은 상태 전이 메서드가 잘 모여 있어 도메인 규칙이 비교적 선명하다.
- 다만 여러 검증이 service에 남아 있어 도메인 객체 응집도는 아직 완전하지 않다.

### 3.5 adapter/out/persistence

- 전부 in-memory 구현이다.
- 빠른 프로토타이핑과 테스트에는 적합하지만, 실제 배포용 저장소/락/트랜잭션 설계는 아직 없다.
- `OccurrenceRepository.getOrCreate`가 기본 capacity 10짜리 회차를 자동 생성하는 점은 데모용 동작에 가깝다.

## 4. 컴포넌트별 특징

### 4.1 BookingCommandController / BookingCommandService

- 프로젝트의 핵심 축이다.
- 예약 생성, 회차 취소/종료, 예약 승인/거절/취소/완료, offer 수락/거절, 인원 감소를 모두 담당한다.
- 강점
  - 멱등성 처리 일관성
  - 상태 전이 규칙이 비교적 명확함
  - audit append 포함
  - integration test가 가장 많이 붙어 있음
- 한계
  - 조회 API가 거의 없음
  - 결제/재고/알림 외부 연동은 없음
  - 실제 DB/락 없이 동시성 시나리오를 강하게 검증하긴 어려움

### 4.2 InquiryCommandController / InquiryCommandService / InquiryQueryService

- 예약 후 고객 문의 쓰레드 기능이다.
- booking 리더와 조직 운영자 간 커뮤니케이션을 겨냥한 구조다.
- 강점
  - 접근 정책 분리가 되어 있음
  - 생성/메시지/종료/조회 최소 플로우는 연결됨
- 한계
  - cursor, limit 파라미터는 받지만 실제 페이징은 구현되지 않음
  - 최초 inquiry 생성 시 `message`를 받지만 저장에는 활용되지 않음
  - 단건 inquiry 조회 API가 없음

### 4.3 ReviewController / ReviewCommandService / ReviewQueryService

- 후기 작성과 요약 조회를 제공한다.
- 참석 완료 사용자만 작성 가능하게 막아둔 점이 핵심이다.
- 강점
  - 자격 검증과 중복 방지 로직이 있음
  - 투어/강사 후기 구분이 명확함
- 한계
  - 단건 review 조회/수정/삭제 없음
  - 요약은 단순 평균/개수만 제공
  - 실제로는 booking 완료가 선행돼야 하므로 booking 흐름 의존도가 높음

### 4.4 RequestHeaderAuthzGuardAdapter

- 인증 시스템 대신 헤더 기반으로 actor를 검증한다.
- 테스트와 초기 API 계약 검증에는 유용하지만, 실제 인증/권한 체계 대체물은 아니다.

### 4.5 GlobalExceptionHandler

- `DomainException`과 일반 유효성 예외를 분리해서 응답한다.
- 에러 포맷 표준화는 되어 있지만, 예외 종류 세분화와 로깅 전략은 아직 얕다.

### 4.6 UserCommandService / UserQueryAdapter

- 가장 미완성인 영역이다.
- 이메일 중복 체크 후 사용자 생성이라는 최소 로직만 있고, persistence adapter는 `TODO()` 상태다.
- 현재 애플리케이션의 핵심 플로우에는 사실상 연결되지 않는다.

## 5. 현재 개발 진척도 평가

### 5.1 완료도가 높은 영역

- 예약 생성/변경/종료 관련 command API
- 멱등성 처리
- 에러 응답 표준화
- 환경 프로파일 분리(`local`, `alpha`, `beta`, `real`)
- booking/review/inquiry 일부 통합 테스트

### 5.2 부분 구현 상태인 영역

- inquiry 조회/메시지 기능
  - 기본 플로우는 있으나 페이징/상세 조회 부족
- review 기능
  - 생성/요약은 있으나 읽기 확장성 부족
- 운영 정책 문서와 실제 API 간 동기화
  - `docs/openapi-gap-report.md` 기준 아직 미구현 API가 많이 남아 있음

### 5.3 미완성 또는 스텁 상태

- 실제 DB 연동
- 외부 결제/알림/에셋 연동
- 사용자 저장/조회
- 검색/리포트/캘린더성 API
- 운영자용 정책 관리 API
- OpenAPI 전체 스펙 구현

### 5.4 정량 감각으로 본 진척도

- 아키텍처 뼈대: 약 75%
- 핵심 예약 command 플로우: 약 80%
- inquiry 기능: 약 55%
- review 기능: 약 60%
- user 기능: 약 20%
- 운영/조회/리포트 확장 API: 약 25%
- 인프라 실전 배포 준비도: 약 30%

종합적으로 보면:

- "도메인 규칙을 검증할 수 있는 백엔드 프로토타입" 단계로는 꽤 진척되어 있다.
- 하지만 "실서비스 운영 가능한 백엔드" 기준으로는 아직 중간 이하 단계다.

## 6. 테스트 기준 현재 상태

- `./gradlew test` 실행 결과 `BUILD SUCCESSFUL`
- 현재 테스트는 다음 축에 집중돼 있다.
  - booking 도메인 단위 테스트
  - booking/inquiry/review 포함 일부 통합 시나리오
  - profile 설정 테스트
  - user service 단위 테스트

해석:

- 핵심 규칙 일부는 자동 검증되고 있다.
- 다만 테스트 분포가 booking 쪽에 치우쳐 있고, inquiry/review/user의 경계 조건은 더 보강이 필요하다.

## 7. 리스크와 주의 포인트

### 7.1 프로토타입성 저장소

- 모든 주요 repository가 in-memory이므로 재시작 시 데이터가 사라진다.
- 동시성, 정합성, 트랜잭션 문제를 실제 운영 수준으로 검증하기 어렵다.

### 7.2 문서와 구현 간 차이

- `docs/openapi-gap-report.md`에 따르면 OpenAPI 대비 미구현 연산이 여전히 많다.
- 즉, 현재 코드는 전체 제품 스펙의 일부를 구현한 상태다.

### 7.3 user 영역 미연결

- 사용자 도메인이 존재하지만, 현재 핵심 흐름은 헤더 기반 actor 주입으로 우회하고 있다.
- 실제 계정 체계가 붙으면 구조 조정이 필요할 가능성이 높다.

### 7.4 inquiry 생성 시 첫 메시지 누락 가능성

- `CreateInquiryCommand`는 `message`를 받지만 실제 저장 로직에는 반영되지 않는다.
- 의도된 설계인지, 누락인지 확인이 필요하다.

## 8. 추천 다음 단계

1. in-memory repository를 실제 DB adapter로 교체하고, 테스트를 repository 계층까지 확장
2. inquiry의 초기 메시지 저장 및 cursor pagination 구현
3. review/read API와 booking/read API 추가
4. user adapter `TODO()` 제거 및 실제 인증 체계 연결
5. OpenAPI 문서와 구현을 다시 맞추고 gap report를 갱신
6. 외부 연동(payment/notification/asset)을 port 기반으로 실제 구현

## 9. 최종 판단

- 현재 프로젝트는 "예약 상태 전이와 API 계약 검증"에 초점을 둔 백엔드 프로토타입으로 해석하는 것이 가장 정확하다.
- 코드 구조는 이후 확장을 고려해 나쁘지 않게 잡혀 있다.
- 다만 데이터 저장, 인증, 조회 API, 운영 기능, 외부 연동이 비어 있어 서비스 완성도는 아직 낮다.
- 그래서 현재 진척도는 대략 "핵심 command MVP는 성립, 전체 제품은 아직 확장 중" 수준으로 보는 것이 적절하다.

## 10. 추가 분석: `01_domain_rules.md` 기준 구현 현황

### 10.1 현재 개발된 부분

- Booking 상태 모델 핵심 전이
  - `REQUESTED -> CONFIRMED/REJECTED/CANCELED`
  - `WAITLISTED -> OFFERED/CANCELED`
  - `OFFERED -> CONFIRMED/EXPIRED/CANCELED`
  - `CONFIRMED -> CANCELED/COMPLETED`
- Terminal 상태 차단
  - terminal booking에 대한 재전이는 대부분 차단된다.
- occurrence lifecycle 일부
  - `SCHEDULED -> FINISHED`
  - `SCHEDULED -> CANCELED`
  - `FINISHED` occurrence 신규 예약 차단
  - occurrence cancel 시 non-terminal booking cascade cancel
- capacity / waitlist / offer 일부
  - 예약 생성 시 좌석 부족하면 `WAITLISTED`
  - 좌석이 풀리면 fitting 가능한 waitlist 예약을 `OFFERED`로 승격
  - offer 기본 만료 시간 24시간
  - `partySize` 감소 가능, 증가 불가
- inquiry 최소 규칙
  - inquiry 생성 시 `bookingId` 필수
  - booking과 occurrence 스코프 검증
  - 참여자/운영자 메시지 작성 및 close 지원
- review 최소 규칙
  - review type 분리
  - 중복 review 방지
  - booking이 `COMPLETED`인 사용자만 작성 가능
- idempotency 필수 규칙
  - mutation endpoint 대부분에서 `Idempotency-Key` 강제
  - 동일 key + 동일 payload replay
  - 동일 key + 다른 payload는 422
  - 처리 중 중복요청은 409

### 10.2 개발이 덜된 부분

- Role model 전체
  - `USER/INSTRUCTOR/ORG_MEMBER/ORG_ADMIN/ORG_OWNER` 개념은 문서에 있으나 실제 권한 모델은 헤더 검증 수준이다.
- instructor/tour/organization 모델
  - 도메인 규칙에는 있으나 실제 엔티티/관계/서비스는 거의 없다.
- attendance 분리 모델
  - `AttendanceStatus` enum은 있지만, participant 단위 출석 기록 로직과 API는 없다.
- participant invitation
  - 초대 생성, 수락/거절, 48시간 만료, 6시간 윈도우 차단 모두 미구현이다.
- inquiry 참여자 범위
  - 문서상 "all booking participants" 기준인데, 현재 코드는 booking leader + org operator 중심이며 participant 모델 자체가 없다.
- review eligibility 완전판
  - 문서는 participant attendance 기반인데, 현재는 `COMPLETED booking의 leader` 기준으로 단순화되어 있다.
- cancellation/refund policy rule engine
  - 48시간 경계, 환불 preview endpoint, FULL_REFUND/NO_REFUND 정책 계산은 없다.
- calendar ICS
  - 전혀 없다.
- operator manual waitlist control
  - 특정 booking 강제 offer, skip, admin note 기록 기능이 없다.

### 10.3 해석

- `01_domain_rules.md` 기준으로 보면 핵심 booking command 규칙은 상당 부분 들어왔다.
- 하지만 participant, attendance, payment policy, manual operations, calendar 쪽은 거의 설계 문서 단계에 머물러 있다.

## 11. 추가 분석: `08_operational_policy_tables.md` 기준 구현 현황

### 11.1 현재 개발된 부분

- idempotency policy의 핵심 골격
  - scope를 `(actor_user_id, method, path_template, key)`로 관리
  - replay 시 원본 status/body 반환
  - in-progress duplicate는 `IDEMPOTENCY_IN_PROGRESS`
- audit log 최소 구현
  - append-only 성격의 `InMemoryAuditEventAdapter` 존재
  - booking/inquiry/review mutation마다 audit append 수행
- time handling 일부
  - `Clock` abstraction 사용
  - offer expiry 비교를 application service에서 처리
- refund 정책의 일부 단순형
  - occurrence cancel, reject, offer decline, 일부 cancel에서 `REFUNDED` 처리

### 11.2 개발이 덜된 부분

- Refund Policy Matrix 대부분
  - 48시간 경계 기반 leader cancellation 정책 없음
  - `FULL_REFUND` vs `NO_REFUND` 계산 없음
  - `REFUND_PENDING` 재시도 플로우 없음
- Payment Failure & Compensation Matrix 전체
  - 실제 gateway 호출 자체가 없고, timeout/retry/rollback/operator queue가 없다.
- Idempotency Policy Matrix 세부 에러코드
  - 문서의 `INVALID_BOOKING_STATE`, `PAYMENT_ALREADY_REFUNDED`, `CAPACITY_INSUFFICIENT` 등은 코드와 불일치하거나 미구현이다.
  - 현재 코드는 `INVALID_STATE_TRANSITION`, `BOOKING_TERMINAL_STATE`, `CAPACITY_EXCEEDED` 등 자체 코드 중심이다.
- Time & Timezone Policy 대부분
  - occurrence timezone 저장 없음
  - local time 기준 N시간 경계 계산 없음
  - DST 테스트 없음
- Waitlist Fairness Policy 확장 규칙
  - 현재는 사실상 FIFO+fit이다.
  - `skip_count`, priority boost, starvation 완화 정책은 없다.
- Audit Log Policy 완전판
  - `before_json`, `after_json`, `reason_code`, append-only 보장 저장소는 없다.
  - in-memory이므로 영속 감사 로그가 아니다.
- rollout checklist 항목
  - retry job, TTL purge job, dashboard, distributed lock 등은 없다.

### 11.3 해석

- `08_operational_policy_tables.md`는 운영 가능한 서비스 기준 정책표에 가깝다.
- 현재 구현은 그중 idempotency와 audit의 "형태"만 일부 반영한 상태이고, 결제/환불/배치/운영관제는 사실상 미착수다.

## 12. 추가 분석: `07_test_scenarios.md` 기준 구현 현황

### 12.1 현재 테스트 또는 구현으로 커버되는 부분

- Scenario 1 `Basic Booking Approval`
- Scenario 2 `Booking Rejection`
- Scenario 3 `Waitlist Creation`
- Scenario 5 `Offer Acceptance`
- Scenario 9 `Party Size Reduction`
- Scenario 11 `Review Eligibility`
- Scenario 12 `Occurrence Cancellation`
- Scenario 14 `Invalid Transition Rejected`
- Scenario 15 `Offer Accept After Expiration`
- Scenario 16 `Occurrence Cancellation Precedence`
- Scenario 17 `Inquiry Booking Scope Validation`
- Scenario 18 `Booking Creation Status Decision`
- Scenario 19 `Idempotent Booking Create`
- Scenario 20 `Idempotency Key Reused With Different Payload`

위 항목들은 `BookingControllerIntegrationTest`, `BookingTest` 및 review/inquiry 관련 로직으로 대체로 확인 가능하다.

### 12.2 일부만 구현된 부분

- Scenario 4 `Waitlist Promotion`
  - 좌석 해제 후 fitting 가능한 waitlist 승격 로직은 있다.
  - 다만 문서 수준의 "A skip, B promote" 공정성 시나리오가 테스트에서 명시적으로 충분히 드러나지는 않는다.
- Scenario 10 `Inquiry Messaging`
  - 메시지 저장은 구현돼 있다.
  - 하지만 "notifications sent"는 미구현이다.
- Scenario 24 `Offer Expiration Boundary`
  - 만료 비교 로직은 있지만, 경계값 테스트가 별도 시나리오로 정교하게 정리돼 있지는 않다.

### 12.3 대부분 미구현인 부분

- Scenario 6 `Offer Expiration`
  - background job 기반 자동 만료가 없다.
- Scenario 7 `Participant Invitation`
- Scenario 8 `Invitation Expiration`
- Scenario 13 `Participant Roster Export`
- Scenario 21 `Leader Cancellation Refund Boundary (48h)`
- Scenario 22 `Refund API Failure Compensation`
- Scenario 23 `Invitation Window Boundary (6h)`

### 12.4 테스트 관점 종합 평가

- 현재 테스트는 booking command 규칙에 강하게 집중돼 있다.
- 문서에 있는 E2E 시나리오 전체 대비로 보면 절반 이상이 아직 테스트로 옮겨지지 않았다.
- 특히 배치 작업, 외부 결제 실패 보상, 초대/참여자 흐름, export 기능은 테스트 대상 자체가 아직 구현되지 않았다.

## 13. 세 문서 기준 최종 정리

### 13.1 현재 개발이 많이 된 부분

- booking command 플로우
- idempotency 처리
- occurrence cancel/finish와 booking 상태 전이
- inquiry 최소 생성/메시지/close
- review 생성/요약
- 헤더 기반 actor 검증
- 일부 통합 테스트 자동화

### 13.2 현재 개발이 덜된 부분

- participant / invitation / attendance
- payment gateway / refund compensation / retry jobs
- 운영 정책 세부 구현(timezone, fairness, reason code, manual controls)
- export / calendar / search / report API
- 실제 인증/권한 시스템
- persistent DB / lock / transactional consistency
- 문서에 정의된 전체 E2E 시나리오 구현 및 테스트

### 13.3 결론 보정

- 처음 분석보다 더 분명한 점은, 현재 프로젝트는 "예약/문의/후기의 일부 핵심 command 규칙"은 구현됐지만 "운영정책과 확장 시나리오"는 대부분 아직 비어 있다는 것이다.
- 즉, 제품 스펙 전체 기준 진척도는 문서가 암시하는 범위보다 낮고, 현재 구현 범위는 명확히 좁은 MVP 프로토타입으로 보는 것이 맞다.
