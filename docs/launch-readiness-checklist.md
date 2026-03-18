# Launch Readiness Checklist

이 문서는 Tourwave를 실제 배포하기 전에 운영자가 마지막으로 확인해야 하는 체크리스트와 런북 요약이다.

## 1. Release Gates

- `./gradlew test`가 통과해야 한다.
- `OpenApiContractVerificationTest`가 통과해야 한다.
- real MySQL container smoke test가 CI에서 통과해야 한다.
- `GET /actuator/health`와 `GET /actuator/health/readiness`가 `UP`이어야 한다.
- `GET /actuator/metrics/tourwave.job.execution`에서 worker execution metric이 노출되어야 한다.

## 2. Worker Runtime Checks

- worker distributed lock가 활성화되어 있어야 한다.
- `worker_job_locks` 테이블이 migration 이후 생성되어 있어야 한다.
- 다중 worker 인스턴스에서 동일 job이 중복 실행되지 않는지 확인해야 한다.
- `workerJobs`, `workerJobLocks` actuator health component를 확인해야 한다.

## 3. CI And Contract Checks

- GitHub Actions CI에서 env validation, contract verification, real MySQL smoke, full regression이 순서대로 실행되어야 한다.
- `agent/13_api_status_matrix.md`와 `agent/04_openapi.yaml` 간 drift가 새로 생기면 같은 PR 안에서 정리해야 한다.
- 새 API를 추가하면 controller/integration test, status matrix, OpenAPI, API catalog를 함께 수정해야 한다.

## 4. Incident First Response

- health가 `DOWN`이면 최근 배포와 migration 적용 여부를 먼저 확인한다.
- `workerJobLocks`가 `DOWN`이면 stale lock과 DB 연결 상태를 우선 확인한다.
- `workerJobs`가 `DOWN`이면 마지막 실패 job name과 최근 에러 메시지를 확인한다.
- refund queue 증가나 reconciliation 누락이 보이면 `RefundRetryJob`, `FinanceReconciliationJob`, payment webhook 수신 로그를 함께 점검한다.
