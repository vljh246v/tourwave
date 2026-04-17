# Tourwave

투어/액티비티 운영사를 위한 예약 플랫폼 백엔드. 고객 예약, 운영 실행, 사후 관리(출석/리뷰/환불)를 하나의 도메인으로 다룬다.

## Tech Stack

- Spring Boot 3.3.1, Kotlin 1.9.24 (JDK 17)
- Spring Data JPA, MySQL, Flyway
- Spring Security (JWT), Micrometer + Prometheus
- JUnit 5, Mockito-Kotlin, Testcontainers (MySQL)

## Repository Layout

- `src/main/kotlin/com/demo/tourwave/` — 런타임 코드 (헥사고날: `domain` / `application` / `adapter.in` / `adapter.out` / `bootstrap`)
- `src/main/kotlin/com/demo/tourwaveworker/` — 백그라운드 워커 진입점
- `src/test/kotlin/` — 단위/통합/컨트랙트 테스트
- `docs/` — 스펙 문서 (아래 표 참조)
- `scripts/` — CI/운영 스크립트

## Spec Documents (`docs/`)

| 파일 | 역할 |
|---|---|
| `docs/domain-rules.md` | 비즈니스 규칙 규범 (예약/대기열/오퍼/환불 상태 전이) |
| `docs/schema.md` | MySQL 스키마 규범 |
| `docs/openapi.yaml` | API 계약 SSOT (OpenAPI 3.x) |
| `docs/architecture.md` | 헥사고날 아키텍처 가드레일 + 구현 노트 |
| `docs/policies.md` | 권한 모델 + 운영 정책 + trust surface |
| `docs/testing.md` | 테스트 규약 + 실제 테스트 클래스 인덱스 |
| `docs/operations.md` | 런타임 토폴로지 + 운영 지표 + 환경 매트릭스 + 출시 체크리스트 |

규범 우선순위: `domain-rules.md` > `architecture.md` > `policies.md` > `schema.md` > `openapi.yaml`.

## Runtime Entry Points

- `TourwaveApplication` — API 서버
- `WorkerApplication` — 배치/스케줄러 워커

같은 Gradle 모듈에 공존하지만 운영 관점에서는 `API + background worker` 분리 구조로 취급한다.

## Build and Test

- 전체 테스트: `./gradlew test`
- OpenAPI 계약 회귀: `./gradlew test --tests 'com.demo.tourwave.agent.OpenApiContractVerificationTest'`
- 컨트롤러 drift 가드: `./gradlew test --tests 'com.demo.tourwave.agent.DocumentationBaselineTest'`
- 실 MySQL 컨테이너 회귀: `./gradlew test --tests 'com.demo.tourwave.adapter.out.persistence.jpa.RealMysqlContainerRegressionTest'`

## Development Harness

이 프로젝트는 Claude Code 하네스를 사용합니다.

```bash
/harness-task <task-id> <설명>   # 작업 시작 → 구현 → 검증 → PR
```

| 파일 | 역할 |
|---|---|
| `harness.config.sh` | 빌드/테스트/린트 명령, Git Flow 설정 |
| `ARCHITECTURE.md` | 레이어 규칙 (헥사고날 아키텍처) |
| `docs/golden-principles.md` | 반복 실패에서 승격된 누적 규칙 |
| `docs/escalation-policy.md` | 사람 승인 필수 변경 목록 |
| `docs/exec-plans/` | 태스크별 실행 계획 (active / completed) |

## License

See `LICENSE`.
