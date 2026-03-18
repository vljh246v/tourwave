# Product Delivery Roadmap

이 문서는 Sprint 7~14 구현 프로그램을 통해 현재 Tourwave 코드를 제품 코어 수준까지 끌고 온 delivery 기록 문서다. 문서 목적은 두 가지다.

- 비즈니스 기준의 Sprint 7~14 구현 로드맵을 고정한다.
- Jira에 생성된 Epic, Story, Sub-task 구조의 기준을 보관한다.

Sprint 14 이후에 남아 있는 release gap closure는 `17_release_gap_execution_plan.md`를 따른다.

## 1. Release Goal

출시 기준 MVP는 아래 5개 축이 모두 닫혀야 한다.

- 고객이 회원가입하고 로그인해서 투어를 조회하고 예약할 수 있다.
- 운영자가 조직, 강사, 투어, 회차를 생성하고 관리할 수 있다.
- 예약 이후 참가자, 문의, 출석, 리뷰, 환불이 운영 가능하다.
- 결제/환불 이벤트가 외부 시스템과 연동되고 장애 시 재처리 가능하다.
- 운영 환경에서 metrics, alerting, worker lock, contract verification이 돌아간다.

## 2. Delivery Assumptions

- 기준 프로젝트: Jira `TW`
- Sprint cadence: 2주
- 기본 issue type:
  - Epic: 제품 축 단위
  - Story: 사용자/운영 가치 단위
  - Task: 인프라/문서/운영 작업
  - Subtask: 구현 분해 단위
- 완료 정의:
  - API/DB/테스트/문서가 함께 반영된다.
  - `./gradlew test`가 통과한다.
  - 관련 문서의 drift가 남지 않는다.

## 3. Epic Structure

### EPIC A. Contract and Product Alignment

- 문서와 실제 코드를 일치시킨다.
- OpenAPI, API catalog, 테스트 기준 문서를 운영 가능한 상태로 맞춘다.

### EPIC B. Identity and Account

- signup/login/refresh/logout
- email verification
- `me` profile
- 인증/인가 기반 전환

### EPIC C. Organization and Operator Backoffice

- organization CRUD
- membership/role management
- instructor onboarding
- operator surface

### EPIC D. Tour Authoring and Public Catalog

- tour CRUD
- occurrence authoring
- publish/search/detail/availability
- content and asset attachment

### EPIC E. Booking Experience Hardening

- customer booking list
- waitlist operator visibility
- notification hooks
- calendar export

### EPIC F. Payment and Financial Operations

- payment provider integration boundary
- webhook/callback intake
- reconciliation and retry handling

### EPIC G. Platform Reliability and Release Readiness

- distributed lock
- metrics/alerting
- MySQL container CI
- contract verification

## 4. Sprint Plan

## Sprint 7. Contract Alignment and Delivery Baseline

목표: 문서/계약/테스트를 현재 코드와 맞추고, 이후 제품 개발의 기준선을 고정한다.

Stories:

- S7-1 API status, overview, backlog 문서 정리
- S7-2 OpenAPI drift 정리 및 목표/현재 상태 분리
- S7-3 테스트 추적 문서 보강
- S7-4 Jira delivery skeleton 정립

Detailed tickets:

1. Story: 문서 기준선 재정의
   - 목적: 현재 구현과 비즈니스상 미구현 영역을 분리해 후속 의사결정 혼선을 제거
   - 산출물: `00_overview`, `11_current_implementation_status`, `13_api_status_matrix`, `15_next_development_backlog`
   - 서브태스크:
     - 실제 controller 기준 구현 endpoint 재점검
     - 제품 기준 미구현 기능군 재분류
     - 운영 리스크 섹션 재작성
     - 관련 문서 링크 정리

2. Story: OpenAPI 관리 정책 정리
   - 목적: `04_openapi.yaml`을 target contract로 유지하면서 current truth와 충돌하지 않게 함
   - 수용 기준:
     - current vs target의 차이를 문서에 명시
     - drift report를 아카이브 성격으로 정리
     - 신규 API 작업 시 update order 규칙 문서화
   - 서브태스크:
     - OpenAPI gap report 재작성
     - API catalog auth 설명 정리
     - mutation idempotency 적용 범위 재점검

3. Task: Jira delivery roadmap 작성
   - 목적: 제품 출시까지 필요한 epics, stories, subtasks, sprint 구성 정리
   - 수용 기준:
     - Sprint 7~14 범위 정의
     - 각 sprint별 목표/의존성/완료 기준 포함
     - Jira issue 생성 가능한 상세 단위 포함

## Sprint 8. Identity and Account Foundation

목표: header actor context를 실제 사용자 인증 체계로 대체할 기반을 만든다.

Stories:

1. Story: 사용자 인증 도메인/스키마 설계
   - 수용 기준:
     - users/auth 관련 스키마 추가
     - refresh token 또는 session 전략 확정
     - email verification, password reset 토큰 저장 전략 정의
   - 서브태스크:
     - auth schema migration 작성
     - auth entity/repository 추가
     - auth domain service 추가
     - auth 테스트 fixture 정리

2. Story: signup/login/refresh/logout API 구현
   - 수용 기준:
     - public signup/login 가능
     - access/refresh lifecycle 동작
     - logout 시 refresh 무효화
     - 에러 코드와 보안 정책 문서 반영
   - 서브태스크:
     - signup controller/service
     - login controller/service
     - refresh/logout controller/service
     - integration tests

3. Story: JWT 기반 security adapter 도입
   - 수용 기준:
     - request header actor context를 테스트 전용 fallback으로 축소
     - 운영 프로필에서 JWT 검증 강제
     - role/organization claim 해석 정책 정리
   - 서브태스크:
     - security dependency 추가
     - auth filter / resolver 구현
     - test support adapter 유지
     - security regression tests

4. Story: `GET/PATCH /me` 구현
   - 수용 기준:
     - 내 프로필 조회/수정 가능
     - 탈퇴/비활성화 정책 초안 반영
   - 서브태스크:
     - me query API
     - me update API
     - profile validation
     - integration tests

## Sprint 9. Organization and Membership Operations

목표: 운영 조직이 실제로 팀을 구성하고 권한을 나눌 수 있게 한다.

Stories:

1. Story: organization persistence 도입
   - 수용 기준:
     - organizations 테이블 및 JPA adapter 추가
     - 생성/조회/수정 기본 흐름 지원
   - 서브태스크:
     - schema migration
     - entity/repository
     - command/query service
     - API tests

2. Story: organization membership와 role 관리
   - 수용 기준:
     - owner/admin/member 역할 저장
     - 멤버 초대/등록/역할 변경/비활성화 가능
   - 서브태스크:
     - membership schema
     - membership service
     - role update API
     - authz regression tests

3. Story: operator organization profile API
   - 수용 기준:
     - organization profile, contact, business metadata 관리 가능
     - 공개 organization 조회와 운영자 조회 권한이 분리됨
   - 서브태스크:
     - public org query
     - operator org update
     - validation rules
     - API documentation update

4. Task: 권한 모델 문서와 실제 코드 동기화
   - 서브태스크:
     - `05_authz_model.md` 업데이트
     - organization auth guards 추가
     - access matrix tests

## Sprint 10. Instructor Onboarding and Tour Authoring

목표: 공급자 측 운영 표면을 열어 실제 상품 데이터를 입력할 수 있게 한다.

Stories:

1. Story: instructor registration workflow 구현
   - 수용 기준:
     - 강사 지원/승인/거절 가능
     - organization과 instructor profile 연결 가능
   - 서브태스크:
     - registration schema
     - registration command/query service
     - approve/reject API
     - workflow tests

2. Story: instructor profile CRUD 구현
   - 수용 기준:
     - 강사 프로필 생성/수정/조회 가능
     - 공개 노출 필드와 운영 필드 분리
   - 서브태스크:
     - profile schema
     - me/instructor-profile API
     - public instructor API
     - tests/document update

3. Story: tour CRUD 구현
   - 수용 기준:
     - organization owner/admin이 투어를 생성/수정 가능
     - draft/published 상태 지원
   - 서브태스크:
     - tours schema
     - tour command/query services
     - operator tour APIs
     - publish state tests

4. Story: tour content model 구현
   - 수용 기준:
     - 설명, 포함/불포함, 준비물, 정책, highlights 저장 가능
   - 서브태스크:
     - content schema/value object
     - content update API
     - validation rules
     - serialization tests

## Sprint 11. Occurrence Authoring and Public Catalog

목표: 공개 판매 가능한 회차와 검색 표면을 완성한다.

Stories:

1. Story: occurrence authoring CRUD 구현
   - 수용 기준:
     - tour 하위 occurrence 생성/수정/취소/재조정 가능
     - capacity, timezone, instructor assignment 관리 가능
   - 서브태스크:
     - occurrence authoring API
     - instructor assignment rules
     - reschedule domain rule
     - tests

2. Story: public catalog query API 구현
   - 수용 기준:
     - `GET /tours`, `GET /tours/{tourId}`, `GET /tours/{tourId}/occurrences` 제공
     - published 상태만 노출
   - 서브태스크:
     - catalog query service
     - catalog controllers
     - pagination/filtering
     - public query tests

3. Story: availability and quote API 구현
   - 수용 기준:
     - occurrence availability 조회
     - 예상 가격/환불 정책 설명 조회
   - 서브태스크:
     - availability projection
     - quote policy service
     - controller responses
     - contract tests

4. Story: search API 구현
   - 수용 기준:
     - 지역/날짜/인원 기반 검색 가능
     - 정렬 및 필터 기초 지원
   - 서브태스크:
     - search query model
     - search repository
     - controller
     - query performance baseline

## Sprint 12. Assets, Customer Surface, and Booking UX

목표: 고객 경험을 제품 수준으로 끌어올린다.

Stories:

1. Story: asset upload/complete/attach workflow 구현
   - 수용 기준:
     - upload URL 발급
     - upload complete 처리
     - tour/organization resource attachment 가능
   - 서브태스크:
     - asset schema
     - storage abstraction
     - upload/complete API
     - attach/reorder/delete API

2. Story: customer booking list와 calendar export 구현
   - 수용 기준:
     - `GET /me/bookings`
     - `GET /bookings/{bookingId}/calendar.ics`
     - `GET /occurrences/{occurrenceId}/calendar.ics`
   - 서브태스크:
     - my bookings query
     - ICS generator
     - permission checks
     - integration tests

3. Story: favorites 기능 구현
   - 수용 기준:
     - tour favorite/unfavorite 가능
     - `GET /me/favorites` 제공
   - 서브태스크:
     - favorites schema
     - command/query API
     - tests
     - docs update

4. Story: notifications read model 기초 구현
   - 수용 기준:
     - 예약/문의/환불 이벤트가 알림으로 축적됨
     - read / read-all API 지원
   - 서브태스크:
     - notification schema
     - event publisher hooks
     - me notifications API
     - tests

## Sprint 13. Payment Integration and Financial Operations

목표: 내부 payment ledger를 외부 결제 이벤트와 연결한다.

Stories:

1. Story: payment provider boundary 설계 및 adapter 도입
   - 수용 기준:
     - provider abstraction 정의
     - 승인/취소/환불 호출 boundary 마련
   - 서브태스크:
     - payment provider interface
     - provider adapter stub
     - config/secrets strategy
     - tests

2. Story: payment webhook/callback intake 구현
   - 수용 기준:
     - 외부 승인/취소/실패 이벤트 수신 가능
     - idempotent 처리 보장
   - 서브태스크:
     - webhook controller
     - signature verification
     - event persistence
     - replay-safe tests

3. Story: refund operation hardening
   - 수용 기준:
     - refund failure queue 가시화
     - retry 상태와 운영자 액션 가시성 제공
   - 서브태스크:
     - refund ops query
     - retry policy config
     - operator remediation endpoint
     - tests

4. Story: reconciliation/reporting foundation
   - 수용 기준:
     - booking/payment/refund 상태를 일자별 집계 가능
   - 서브태스크:
     - reconciliation table/projection
     - daily summary job
     - export/query API
     - documentation

## Sprint 14. Reliability, Compliance, and Launch Readiness

목표: 실제 운영 배포 가능한 안정성을 확보한다.

Stories:

1. Story: worker distributed lock 적용
   - 수용 기준:
     - multi-instance 환경에서 중복 실행 방지
     - 잡별 lock policy 문서화
   - 서브태스크:
     - lock strategy 선택
     - job wrapper 적용
     - failure/recovery tests
     - ops doc update

2. Story: metrics/alerting/health 강화
   - 수용 기준:
     - 예약, 결제, 환불, worker 실패 지표 수집
     - 핵심 알림 기준 정의
   - 서브태스크:
     - metrics instrumentation
     - health indicators
     - alert rule draft
     - dashboard docs

3. Task: MySQL container CI와 contract verification
   - 수용 기준:
     - H2 외 real MySQL 테스트 경로 확보
     - OpenAPI parser 기반 contract 검증 추가
   - 서브태스크:
     - testcontainers CI job
     - contract test harness
     - failure gating rule
     - docs update

4. Task: launch readiness checklist
   - 수용 기준:
     - 보안/백업/운영 핸드북/배포 체크리스트 완성
   - 서브태스크:
     - env matrix 정리
     - secret rotation policy
     - rollback checklist
     - launch sign-off template

## 5. Jira Ticketing Rule

- Epic은 위 7개 축으로 고정한다.
- Story는 sprint goal에 직접 연결되는 사용자 가치 또는 운영 가치 단위로 만든다.
- Sub-task는 다음 네 가지 중 2개 이상으로 쪼갠다.
  - schema/persistence
  - application/domain
  - web/worker adapter
  - test/documentation

## 6. Suggested Labels

- `domain-booking`
- `domain-auth`
- `domain-operator`
- `domain-catalog`
- `domain-payment`
- `platform-ops`
- `contract`
- `release-blocker`

## 7. Release Gates

출시 전 마지막 점검 항목:

1. JWT 인증 없이 운영 API 접근이 불가능하다.
2. organization/tour/occurrence authoring이 백오피스에서 닫힌다.
3. public catalog에서 검색 후 예약 생성이 끝까지 연결된다.
4. payment callback과 refund retry가 운영 가능한 수준으로 가시화된다.
5. worker 중복 실행과 주요 장애에 대한 모니터링이 있다.
