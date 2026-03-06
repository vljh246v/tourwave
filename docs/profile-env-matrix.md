# Tourwave Profile Configuration Matrix

다음 정책으로 프로파일을 운영합니다.

- 공통 설정은 `src/main/resources/application.yml`에만 유지
- 환경 차등 설정은 `application-local.yml`, `application-alpha.yml`, `application-beta.yml`, `application-real.yml`에만 정의
- 시크릿/토큰/비밀번호 값은 모두 환경변수 참조로만 주입

## Profile별 설정표

| Profile | `tourwave.environment` | `tourwave.runtime-mode` | DB URL 기본값 | Integration URL 기본값 |
|---|---|---|---|---|
| local | `local` | `local-system` | `jdbc:mysql://localhost:3306/tourwave_local` | payment/notification/asset 모두 localhost mock 기본값 제공 |
| alpha | `alpha` | `shared-alpha` | 없음(필수 env) | 없음(필수 env) |
| beta | `beta` | `pre-production` | 없음(필수 env) | 없음(필수 env) |
| real | `real` | `production` | 없음(필수 env) | 없음(필수 env) |

## 공통 환경변수

- `IDEMPOTENCY_TTL_SECONDS` (선택, 기본값 `86400`)

## local 환경변수

- `LOCAL_DB_URL` (선택, 기본값 `jdbc:mysql://localhost:3306/tourwave_local`)
- `LOCAL_DB_USERNAME` (선택)
- `LOCAL_DB_PASSWORD` (선택)
- `LOCAL_PAYMENT_BASE_URL` (선택, 기본값 `http://localhost:18080/mock-payment`)
- `LOCAL_PAYMENT_API_KEY` (선택)
- `LOCAL_NOTIFICATION_BASE_URL` (선택, 기본값 `http://localhost:18081/mock-notification`)
- `LOCAL_NOTIFICATION_API_KEY` (선택)
- `LOCAL_ASSET_BASE_URL` (선택, 기본값 `http://localhost:18082/mock-asset`)
- `LOCAL_ASSET_ACCESS_TOKEN` (선택)

## alpha 필수 환경변수

- `ALPHA_DB_URL`
- `ALPHA_DB_USERNAME`
- `ALPHA_DB_PASSWORD`
- `ALPHA_PAYMENT_BASE_URL`
- `ALPHA_PAYMENT_API_KEY`
- `ALPHA_NOTIFICATION_BASE_URL`
- `ALPHA_NOTIFICATION_API_KEY`
- `ALPHA_ASSET_BASE_URL`
- `ALPHA_ASSET_ACCESS_TOKEN`

## beta 필수 환경변수

- `BETA_DB_URL`
- `BETA_DB_USERNAME`
- `BETA_DB_PASSWORD`
- `BETA_PAYMENT_BASE_URL`
- `BETA_PAYMENT_API_KEY`
- `BETA_NOTIFICATION_BASE_URL`
- `BETA_NOTIFICATION_API_KEY`
- `BETA_ASSET_BASE_URL`
- `BETA_ASSET_ACCESS_TOKEN`

## real 필수 환경변수

- `REAL_DB_URL`
- `REAL_DB_USERNAME`
- `REAL_DB_PASSWORD`
- `REAL_PAYMENT_BASE_URL`
- `REAL_PAYMENT_API_KEY`
- `REAL_NOTIFICATION_BASE_URL`
- `REAL_NOTIFICATION_API_KEY`
- `REAL_ASSET_BASE_URL`
- `REAL_ASSET_ACCESS_TOKEN`
