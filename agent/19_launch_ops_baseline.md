# Launch Ops Baseline

이 문서는 Sprint 20 기준으로 Tourwave의 운영 관측, alert routing, SLO, remediation queue, real MySQL verification baseline을 고정한다.

## 1. Metrics Inventory

- `tourwave.job.execution`
  - tag: `job`, `result`
  - 목적: worker job success/failure/skip 추적
- `tourwave.job.execution.duration`
  - tag: `job`, `result`
  - 목적: job latency 추적
- `tourwave.job.lock.skipped`
  - tag: `job`
  - 목적: distributed lock skip 급증 감지
- actuator health
  - component: `workerJobs`, `workerJobLocks`, `liveness`, `readiness`
- payment/webhook 운영 지표 source
  - `PaymentProviderEventStatus.REJECTED_SIGNATURE`
  - `PaymentProviderEventStatus.MALFORMED_PAYLOAD`
  - `PaymentProviderEventStatus.POISONED`
- remediation queue source
  - refund failure / review-required
  - notification delivery failure
  - poisoned or rejected webhook event

## 2. Alert Routing Draft

- P1 page
  - `GET /actuator/health/readiness != UP` 5분 이상
  - `workerJobLocks != UP` 5분 이상
  - `tourwave.job.execution{result="failure"}` 최근 15분 내 3회 이상
- P2 Slack / email
  - `tourwave.job.lock.skipped` 급증
  - webhook `REJECTED_SIGNATURE` 또는 `POISONED` 1시간 내 1회 이상
  - remediation queue open item 증가
- Finance operator review
  - refund queue `REFUND_REVIEW_REQUIRED` 증가
  - reconciliation mismatch count 증가

## 3. Dashboard Baseline

- API health panel
  - readiness, liveness, `workerJobs`, `workerJobLocks`
- Worker execution panel
  - job success/failure/skip count
  - p95 execution duration
- Payment ops panel
  - webhook rejected/malformed/poisoned count
  - refund retryable/review-required count
  - reconciliation mismatch count
- Communication ops panel
  - notification delivery failure count
  - remediation queue open items by source type

## 4. SLO Draft

- API availability SLO
  - 목표: 99.5% monthly availability
  - source: readiness success rate
- Booking mutation success SLO
  - 목표: 99.0% successful completion excluding validation error
  - source: application/API logs + booking mutation integration regression
- Worker execution freshness SLO
  - 목표: scheduled job backlog 15분 이내
  - source: job execution timestamp and skip/failure metrics

Error budget draft:

- API availability monthly budget: 0.5%
- booking mutation monthly budget: 1.0%
- worker freshness monthly budget: 15분 초과 지연 누적 4시간 이하

## 5. Remediation Queue Rule

- queue path: `GET /operator/operations/remediation-queue`
- manual action path: `POST /operator/operations/remediation-queue/{sourceType}/{sourceKey}`
- source type
  - `REFUND`
  - `NOTIFICATION_DELIVERY`
  - `PAYMENT_WEBHOOK`
- action
  - `RETRY`
    - refund retryable failure
    - retryable notification delivery failure
    - poisoned webhook reprocess
  - `RESOLVE`
    - operator acknowledgement / manual closure
- manual action metadata는 `operator_failure_records`에 저장한다.
- operator action audit event는 `OPERATOR_FAILURE_RETRY`, `OPERATOR_FAILURE_RESOLVE`로 남긴다.

## 6. Real MySQL Verification Baseline

- CI mandatory classes
  - `RealMysqlContainerSmokeTest`
  - `RealMysqlContainerRegressionTest`
- local H2 compatibility regression은 `mysql-test` profile로 계속 유지한다.
- flaky rule
  - Docker unavailable 환경에서는 Testcontainers suite는 skip 가능
  - 로직 flaky는 허용하지 않는다. 시간/순서 의존 테스트는 fixed clock 또는 deterministic ordering으로 고정한다.
  - real MySQL regression 실패 시 launch evidence를 승인하지 않는다.
