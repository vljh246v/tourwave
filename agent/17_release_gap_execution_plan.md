# Release Gap Execution Plan

이 문서는 Sprint 14 완료 이후 기준으로, Tourwave를 실제 운영 가능한 제품 수준으로 끌고 가기 위해 남은 gap을 실행 항목으로 쪼갠 문서다. 이미 구현된 Sprint 7~14 범위는 `16_product_delivery_roadmap.md`를 따르고, 이 문서는 그 이후의 release blocker closure만 다룬다.

## 1. Current Gap Summary

현재 코드 기준으로 제품 출시를 막는 큰 축은 아래와 같다.

- security perimeter가 아직 Spring Security filter chain에서 강제되지 않는다.
- email verification / password reset / account deactivation lifecycle이 닫히지 않았다.
- asset storage, payment provider, outbound notification이 real integration이 아니라 fake/stub/internal read model 수준이다.
- target contract 문서에는 announcements / reports / moderation / richer review summary가 남아 있지만 runtime 구현은 없다.
- alert routing / dashboard / SLO / broader MySQL verification이 launch 운영 수준으로 닫히지 않았다.

## 2. Delivery Principles

- 구현 순서는 출시 차단 항목부터 닫는다.
- 문서에서 target-only 항목은 current runtime으로 오해되지 않게 명확히 분리한다.
- 각 Story는 최소한 `schema or config`, `application`, `adapter`, `tests/docs` 단위의 서브태스크를 가진다.
- 완료 기준은 코드 + 테스트 + 문서 + 운영 설정까지 포함한다.

## 3. Stage Plan

### Sprint 15. Security Perimeter And Account Recovery

목표: 인증을 perimeter에서 강제하고 계정 복구 흐름을 닫아 제품 계정 표면을 완성한다.

Epic:
- EPIC H. Security Perimeter and Account Lifecycle

Stories:

1. Story: Spring Security route policy hardening
   - 목적: 현재 `permitAll` 기반 구조를 공개/보호 경로로 명확히 분리
   - 수용 기준:
     - public endpoint와 protected endpoint whitelist/blacklist 정의
     - protected route는 JWT 없으면 401
     - local/test fallback은 test profile로만 제한
     - controller guard 누락이 있더라도 perimeter에서 1차 차단
   - 서브태스크:
     - security config route matrix 정의
     - JWT auth filter 및 exception mapping 정리
     - local/test fallback scope 축소
     - security regression integration tests

2. Story: Email verification flow 구현
   - 목적: signup 이후 이메일 검증 전용 lifecycle 완성
   - 수용 기준:
     - verify-request / verify-confirm API 구현
     - token expiry / replay / already-verified 처리
     - verified flag와 audit event 반영
     - user enumeration 방지 규칙 문서화
   - 서브태스크:
     - action token query/command 정리
     - email verify application service 추가
     - auth controller endpoint 추가
     - service/integration tests 및 docs update

3. Story: Password reset flow 구현
   - 목적: 계정 복구 최소 기능 제공
   - 수용 기준:
     - reset-request / reset-confirm API 구현
     - reset token 만료/재사용 방지
     - 비밀번호 변경 후 refresh token 무효화
     - 보안 이벤트 문서화
   - 서브태스크:
     - password reset token lifecycle 구현
     - password update + session invalidation 처리
     - reset controller/integration tests
     - threat model / docs update

4. Story: Account deactivation and session revocation
   - 목적: 사용자 비활성화 정책을 닫고 토큰 정리 기준을 만든다.
   - 수용 기준:
     - `/me/deactivate` 구현
     - deactivated account login/refresh 차단
     - organization/instructor data visibility 정책 정의
     - regression tests 추가
   - 서브태스크:
     - deactivate domain policy 추가
     - auth/session invalidation 연동
     - API/validation tests
     - operator/customer policy docs update

### Sprint 16. Real Storage And Communication Delivery

목표: fake asset storage와 internal-only notification 모델을 실서비스 연동 가능한 구조로 교체한다.

Epic:
- EPIC I. External Delivery Integrations

Stories:

1. Story: Real asset storage adapter 도입
   - 목적: signed upload URL과 completion 검증을 실제 object storage 기준으로 전환
   - 수용 기준:
     - storage provider abstraction 확정
     - presigned upload URL 발급
     - complete 시 object existence / metadata 검증
     - fake adapter는 local/test fallback으로만 유지
   - 서브태스크:
     - storage config/secrets model 정의
     - provider adapter 구현
     - asset complete validation 보강
     - integration tests 및 profile docs update

2. Story: Notification channel abstraction 구현
   - 목적: 현재 notification read model과 외부 발송 채널을 분리
   - 수용 기준:
     - email/SMS/push 중 최소 1개 채널 adapter boundary 도입
     - read model 저장과 outbound delivery 분리
     - retryable / non-retryable failure 분류
     - message template 관리 기준 정의
   - 서브태스크:
     - notification outbox or delivery command model
     - provider client adapter
     - template / payload mapping
     - service/tests/docs update

3. Story: Organization invitation delivery flow
   - 목적: 현재 membership 생성만 되는 초대를 실제 전달 흐름으로 연결
   - 수용 기준:
     - invite 생성 시 이메일 발송 이벤트 생성
     - invite accept UX에 필요한 token/link 정책 정의
     - resend / expired 처리 규칙 정리
   - 서브태스크:
     - invitation delivery payload 설계
     - invite mail trigger 추가
     - accept token validation 보강
     - invitation tests/docs update

4. Task: Runtime profile and secret management 정리
   - 목적: real/local/test adapter 전환 기준을 운영 문서와 설정에 반영
   - 수용 기준:
     - env/profile matrix 업데이트
     - required secret 목록 정리
     - launch checklist 반영
   - 서브태스크:
     - profile docs update
     - application config audit
     - missing secret fail-fast 정책 정의

### Sprint 17. Real Payment Provider Cutover

목표: payment boundary를 실제 PG 연동 수준으로 전환한다.

Epic:
- EPIC J. Real Payment and Refund Integration

Stories:

1. Story: Payment provider authorize/capture/refund adapter 구현
   - 목적: stub-pay를 실제 provider adapter로 치환
   - 수용 기준:
     - authorize/capture/refund command mapping
     - provider transaction id 저장
     - provider error to domain error mapping
     - sandbox/test adapter 전략 정의
   - 서브태스크:
     - provider client/config 구현
     - payment ledger mapping 확장
     - failure taxonomy 정리
     - integration tests/docs update

2. Story: Webhook authenticity and replay ops hardening
   - 목적: webhook 수신을 운영 수준으로 끌어올림
   - 수용 기준:
     - secret rotation 전략 문서화
     - replay detection / poison payload 처리
     - invalid signature metrics/alert 기준 정의
     - raw payload 보관 정책 정리
   - 서브태스크:
     - signature verifier rotation 지원
     - webhook audit persistence 정리
     - malformed event handling tests
     - ops docs/alerts update

3. Story: Refund remediation queue hardening
   - 목적: 수동 재처리와 자동 재시도 사이의 운영 가시성 확보
   - 수용 기준:
     - retry attempt / next retry / last error를 조회 가능
     - review-required queue를 명시적으로 노출
     - operator action audit trail 저장
   - 서브태스크:
     - refund ops projection 확장
     - remediation endpoint payload 확장
     - operator audit tests
     - queue docs update

4. Story: Payment reconciliation accuracy 확장
   - 목적: 단순 건수 집계가 아니라 provider 정합성 검증 기반으로 확장
   - 수용 기준:
     - booking/payment/refund reconciliation mismatch 식별
     - mismatch export와 refresh 정책 제공
     - finance runbook 반영
   - 서브태스크:
     - reconciliation schema/projection 확장
     - mismatch query/export API
     - scheduled refresh tests
     - finance docs update

### Sprint 18. Product Communication And Reporting Surface

목표: 운영자가 고객과 소통하고 기본 보고서를 조회할 수 있게 한다.

Epic:
- EPIC K. Operator Communication and Reporting

Stories:

1. Story: Announcement domain and public/operator APIs
   - 목적: target-only 상태인 announcements를 실제 runtime으로 전환
   - 수용 기준:
     - announcement schema와 CRUD 구현
     - organization operator create/update/delete
     - public published announcements 조회
     - visibility/publish window 규칙 문서화
   - 서브태스크:
     - schema/entity/repository
     - application service
     - operator/public controller
     - tests/docs update

2. Story: Organization booking report API
   - 목적: 운영자가 상품/회차/예약 기준 운영 리포트를 조회
   - 수용 기준:
     - booking report query API 구현
     - date/org/tour/occurrence filter 지원
     - CSV export 제공
     - authz 및 성능 baseline 확보
   - 서브태스크:
     - report projection/query model
     - booking report controller
     - CSV/export tests
     - docs update

3. Story: Organization occurrence ops report API
   - 목적: 회차 운영 현황을 capacity/booking/attendance 기준으로 조회
   - 수용 기준:
     - occurrence report query API 구현
     - seat utilization / attendance / refund signal 포함
     - CSV export 제공
   - 서브태스크:
     - occurrence report projection
     - query/export endpoint
     - integration tests
     - report glossary docs

4. Task: Reporting and announcement authorization alignment
   - 목적: operator 권한 매트릭스를 새 surface에 맞춰 정리
   - 수용 기준:
     - `05_authz_model.md` 반영
     - regression tests 추가
   - 서브태스크:
     - authz model update
     - report/announcement access tests

### Sprint 19. Review Aggregation And Moderation Decision Closure

목표: 현재 occurrence-level에 묶여 있는 public trust surface를 확장하고 moderation 범위를 명확히 한다.

Epic:
- EPIC L. Trust Surface and Policy Closure

Stories:

1. Story: Tour and instructor review aggregation
   - 목적: public catalog에서 entity-level 평판 표시 지원
   - 수용 기준:
     - `tourId` 기반 rating summary
     - `instructorProfileId` 기반 rating summary
     - aggregation refresh policy 정의
   - 서브태스크:
     - aggregation query/projection
     - public controller endpoints
     - consistency tests
     - docs update

2. Story: Organization review aggregation
   - 목적: 운영 주체 수준 평판 조회 지원
   - 수용 기준:
     - `organizationId` 기준 summary 제공
     - public/operator visibility 범위 정의
   - 서브태스크:
     - organization aggregation query
     - API/response schema
     - tests/docs update

3. Task: Moderation policy decision
   - 목적: moderation이 실제 필요한지, 필요하다면 최소 범위를 고정
   - 수용 기준:
     - no-build or build decision 문서화
     - build하는 경우 최소 API와 role 정의
     - target contract 정리
   - 서브태스크:
     - policy memo 작성
     - authz impact review
     - backlog/openapi update

4. Task: Favorites and notifications UX hardening
   - 목적: 이미 구현된 customer surface의 운영 품질을 높임
   - 수용 기준:
     - unread count or cursor 정책 정의
     - favorite list sorting/filtering 정리
     - noisy notification suppression 규칙 정의
   - 서브태스크:
     - customer UX rules doc
     - query enhancement backlog
     - tests if surface changes

### Sprint 20. Launch Ops And Verification Closure

목표: 운영 지표, alerting, MySQL verification, launch checklist를 실제 배포 기준으로 닫는다.

Epic:
- EPIC M. Launch Operations and Verification

Stories:

1. Story: Alert routing and SLO dashboard baseline
   - 목적: actuator/metrics를 실제 운영 대응 체계로 연결
   - 수용 기준:
     - 핵심 alert rule 정의
     - booking/payment/refund/job 실패 dashboard 구성 기준 작성
     - SLO / error budget 초안 작성
   - 서브태스크:
     - metrics inventory update
     - alert threshold draft
     - dashboard/runbook docs
     - verification checklist

2. Story: Real MySQL verification suite expansion
   - 목적: 현재 smoke 수준 검증을 더 넓은 회귀로 확장
   - 수용 기준:
     - 핵심 persistence suite가 real MySQL에서 실행
     - CI job 분리 또는 matrix 정리
     - flaky 대응 규칙 문서화
   - 서브태스크:
     - MySQL suite selection
     - CI workflow update
     - flaky test policy
     - docs/tests update

3. Story: Operator dead-letter and remediation queue baseline
   - 목적: payment/notification/webhook 실패를 운영자가 추적 가능하게 함
   - 수용 기준:
     - 실패 큐 또는 equivalent projection 정의
     - replay/manual resolve 흐름 정의
     - audit trail 저장
   - 서브태스크:
     - failure queue model
     - operator query/action endpoints
     - tests/docs update

4. Task: Launch readiness checklist closure
   - 목적: 실제 출시 전 확인 항목을 코드/문서 기준으로 잠근다.
   - 수용 기준:
     - secret rotation, backup, incident contact, rollback, smoke test 포함
     - checklist owner와 evidence 기준 명시
   - 서브태스크:
     - checklist rewrite
     - release evidence template
     - post-deploy smoke runbook

## 4. Ticket Writing Rules

- Story summary는 `S15: ...` 같은 sprint prefix를 붙인다.
- Subtask는 구현 단위를 바로 알 수 있게 동사형으로 쓴다.
- Story description에는 목적, 수용 기준, dependencies, completion evidence를 모두 적는다.
- Subtask description에는 touched layer와 expected test/doc update를 명시한다.

## 5. Jira Creation Mapping

새 Jira 생성 단위는 아래 순서를 따른다.

1. Sprint 15~20 생성
2. Epic H~M 생성
3. 각 Sprint별 Story/Task 생성
4. 각 Story/Task 하위 Subtask 생성
5. Sprint 배치와 dependency comment 추가
