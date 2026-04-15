# Operations

Runtime topology, observability, profile configuration, and launch readiness for the Tour Booking Platform.

---

## 1. Runtime Topology

### Entry Points

| Mode | Main Class | Role |
|---|---|---|
| API | `com.demo.tourwave.TourwaveApplication` | HTTP API, web adapters, application service orchestration |
| Worker | `com.demo.tourwaveworker.WorkerApplication` | Scheduled jobs: offer/invitation expiration, refund retry, idempotency purge, distributed lock execution |

Both modes run from the same codebase and share domain/application/persistence code. Current state: one Gradle module, two runtime entrypoints (Gradle multi-module split is pending — see §5).

### Worker Design Rules

- Jobs must not implement domain logic — all rules live in `application` services.
- Every scheduled job runs through `ScheduledJobCoordinator` (distributed lock + metrics + skip/failure tracking).
- To add a new job: create the `application` use case first, then add the `adapter.in.job` entrypoint.

### Distributed Lock Configuration

| Variable | Default | Purpose |
|---|---|---|
| `TOURWAVE_JOB_LOCK_OWNER_ID` | (generated) | Worker instance lock owner ID |
| `TOURWAVE_JOB_LOCK_LEASE_SECONDS` | `120` | Lock lease duration |
| `TOURWAVE_JOB_LOCK_STALE_AFTER_SECONDS` | `300` | Stale lock threshold |

Lock storage: `WorkerJobLockRepository` — in-memory for `local`/`test`; JPA/MySQL for `alpha`+.

### Persistence Profiles

| Profile | Adapter | Notes |
|---|---|---|
| `local` / default | In-memory | No Docker. Fake asset/notification/payment adapters. Auth fallback enabled. |
| `mysql-test` | JPA + H2 MySQL-compat | For CI environments without Docker. |
| `mysql` | JPA + real MySQL | Flyway migrations run. Real datasource. |

### Fail-Fast Rule

`alpha`, `beta`, `real` profiles require all environment variables. A missing required secret causes Spring property binding to fail at startup — no runtime fallback. Only `local`/`test` allow default placeholder values.

### AWS Deployment Target

- API: ECS/Fargate always-on service.
- Worker: separate ECS/Fargate service or EventBridge scheduled tasks.
- Shared: same DB instance and domain codebase.

---

## 2. Observability & Alerts

### Metrics Inventory

| Metric | Tags | Purpose |
|---|---|---|
| `tourwave.job.execution` | `job`, `result` | Worker job success/failure/skip count |
| `tourwave.job.execution.duration` | `job`, `result` | Job latency |
| `tourwave.job.lock.skipped` | `job` | Distributed lock skip spike detection |
| Actuator health components | `workerJobs`, `workerJobLocks`, `liveness`, `readiness` | Runtime health |
| Payment webhook | `REJECTED_SIGNATURE`, `MALFORMED_PAYLOAD`, `POISONED` | Webhook integrity |
| Remediation queue | `REFUND`, `NOTIFICATION_DELIVERY`, `PAYMENT_WEBHOOK` | Operator ops backlog |

### Alert Routing Draft

| Severity | Condition | Channel |
|---|---|---|
| P1 page | `GET /actuator/health/readiness != UP` for 5+ min | On-call |
| P1 page | `workerJobLocks != UP` for 5+ min | On-call |
| P1 page | `tourwave.job.execution{result="failure"}` ≥ 3 in last 15 min | On-call |
| P2 Slack | `tourwave.job.lock.skipped` spike | Ops |
| P2 Slack | `REJECTED_SIGNATURE` or `POISONED` webhook ≥ 1 in 1h | Ops |
| P2 Slack | Remediation queue open items growing | Ops |
| Finance review | `REFUND_REVIEW_REQUIRED` count growing | Finance operator |
| Finance review | Reconciliation mismatch count growing | Finance operator |

### Dashboard Panels

- **API health**: readiness, liveness, `workerJobs`, `workerJobLocks`
- **Worker execution**: job success/failure/skip count, p95 duration
- **Payment ops**: webhook rejected/malformed/poisoned, refund retryable/review-required, reconciliation mismatch
- **Communication ops**: notification delivery failures, remediation queue open items by source type

### SLO Draft

| SLO | Target | Source |
|---|---|---|
| API availability | 99.5% monthly | Readiness success rate |
| Booking mutation success | 99.0% (excluding validation errors) | API logs + booking regression |
| Worker freshness | Backlog cleared within 15 min | Job execution metrics |

Error budgets: API 0.5% monthly · Booking mutation 1.0% monthly · Worker freshness ≤ 4h cumulative overrun/month.

### Remediation Queue

| Field | Value |
|---|---|
| List | `GET /operator/operations/remediation-queue` |
| Manual action | `POST /operator/operations/remediation-queue/{sourceType}/{sourceKey}` |
| Source types | `REFUND`, `NOTIFICATION_DELIVERY`, `PAYMENT_WEBHOOK` |
| Actions | `RETRY` (retryable failure) · `RESOLVE` (manual acknowledgement/close) |
| Audit events written | `OPERATOR_FAILURE_RETRY`, `OPERATOR_FAILURE_RESOLVE` |
| Storage | `operator_failure_records` |

---

## 3. Profile & Environment Matrix

### Profile Summary

| Profile | `tourwave.environment` | `tourwave.runtime-mode` | DB default | Integration defaults |
|---|---|---|---|---|
| `local` | `local` | `local-system` | `jdbc:mysql://localhost:3306/tourwave_local` | localhost mock defaults provided |
| `alpha` | `alpha` | `shared-alpha` | required env | required env |
| `beta` | `beta` | `pre-production` | required env | required env |
| `real` | `real` | `production` | required env | required env |

Config rules:
- Common settings in `src/main/resources/application.yml` only.
- Environment-specific settings in `application-{profile}.yml` only.
- Secret values injected via environment variable references — never hardcoded.

CI env validation: `scripts/check-required-env.sh {alpha|beta|real}`. Exit `0` = OK · `1` = `E_MISSING_REQUIRED_ENV` · `2` = `E_USAGE/E_UNSUPPORTED_PROFILE`. Key names only are logged; values are never printed.

### Common Environment Variables (Optional)

| Variable | Default | Purpose |
|---|---|---|
| `IDEMPOTENCY_TTL_SECONDS` | `86400` | Idempotency key TTL |
| `TOURWAVE_JOB_LOCK_OWNER_ID` | (generated) | Worker instance owner ID override |
| `TOURWAVE_JOB_LOCK_LEASE_SECONDS` | `120` | Lock lease duration |
| `TOURWAVE_JOB_LOCK_STALE_AFTER_SECONDS` | `300` | Lock stale threshold |

### Required Secrets per Environment

`local` variables are optional (defaults provided). `alpha`/`beta`/`real` variables are required (fail-fast on missing). Replace `{ENV}` with `ALPHA`, `BETA`, or `REAL`.

| Secret | local variable | alpha/beta/real variable |
|---|---|---|
| DB URL | `LOCAL_DB_URL` | `{ENV}_DB_URL` |
| DB username | `LOCAL_DB_USERNAME` | `{ENV}_DB_USERNAME` |
| DB password | `LOCAL_DB_PASSWORD` | `{ENV}_DB_PASSWORD` |
| Payment base URL | `LOCAL_PAYMENT_BASE_URL` | `{ENV}_PAYMENT_BASE_URL` |
| Payment API key | `LOCAL_PAYMENT_API_KEY` | `{ENV}_PAYMENT_API_KEY` |
| Notification base URL | `LOCAL_NOTIFICATION_BASE_URL` | `{ENV}_NOTIFICATION_BASE_URL` |
| Notification API key | `LOCAL_NOTIFICATION_API_KEY` | `{ENV}_NOTIFICATION_API_KEY` |
| Asset base URL | `LOCAL_ASSET_BASE_URL` | `{ENV}_ASSET_BASE_URL` |
| Asset access token | `LOCAL_ASSET_ACCESS_TOKEN` | `{ENV}_ASSET_ACCESS_TOKEN` |

---

## 4. Launch Readiness Checklist

### Release Gates (all must pass before deploy)

- [ ] `./gradlew test` passes
- [ ] `OpenApiContractVerificationTest` passes
- [ ] `RealMysqlContainerSmokeTest` + `RealMysqlContainerRegressionTest` pass in CI
- [ ] `GET /actuator/health` and `/actuator/health/readiness` return `UP`
- [ ] `GET /actuator/metrics/tourwave.job.execution` exposes worker metrics
- [ ] `GET /operator/operations/remediation-queue` returns without error; launch-blocking items resolved

### Evidence Owners

| Owner | Evidence Required |
|---|---|
| Backend | Full regression pass log, OpenAPI verification pass, MySQL smoke log |
| Platform | Secret rotation evidence, backup evidence, incident contact confirmed |
| Finance/Operator | Reconciliation refresh evidence, remediation queue review |

### Worker Runtime Checks

- [ ] `worker_job_locks` table exists after Flyway migration
- [ ] Distributed lock is active; no duplicate job execution across multiple worker instances
- [ ] `workerJobs` and `workerJobLocks` actuator health components both `UP`

### CI Pipeline Order

1. Env validation (`scripts/check-required-env.sh`)
2. Contract verification (`OpenApiContractVerificationTest`)
3. Real MySQL smoke (`RealMysqlContainerSmokeTest`)
4. Real MySQL regression (`RealMysqlContainerRegressionTest`)
5. Full regression (`./gradlew test`)

### Post-Deploy Smoke Runbook

```
GET  /actuator/health
GET  /actuator/health/readiness
GET  /actuator/metrics/tourwave.job.execution
GET  /operator/operations/remediation-queue
POST /operator/finance/reconciliation/daily/{summaryDate}/refresh
```

Record: deploy timestamp, response results, reviewer name.

---

## 5. Incident Runbook Pointers

| Symptom | First Check |
|---|---|
| Health `DOWN` | Recent deploy log + migration apply status |
| `workerJobLocks DOWN` | Stale lock rows in `worker_job_locks` + DB connection pool |
| `workerJobs DOWN` | Last failed job name + recent error log |
| Refund queue growing | `RefundRetryJob`, `FinanceReconciliationJob`, payment webhook receive log |
| `PAYMENT_WEBHOOK` remediation items growing | Separate `REJECTED_SIGNATURE` vs `MALFORMED_PAYLOAD` vs `POISONED` by source |
| `NOTIFICATION_DELIVERY` remediation items growing | Provider outage vs permanent bounce — separate by failure category |

### Pending Infra Work

- Gradle true multi-module split (runtime separation done; build module split pending)
- Worker schedule ownership standardization
- Webhook invalid-signature / poison-event alert automation wiring (Pager/Slack)

---

## Cross-References

| Document | Content |
|---|---|
| `docs/testing.md §6` | Test commands for regression, smoke, and contract verification |
| `docs/policies.md §4.2` | Payment failure compensation and refund retry rules |
| `docs/policies.md §4.6` | Audit log policy (append-only, operator actions) |
| `docs/architecture.md` | Hexagonal module structure and Spring profile wiring |
