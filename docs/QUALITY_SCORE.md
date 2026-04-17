# Quality Score — 도메인별 품질 현황

GC 에이전트 또는 수동으로 업데이트합니다.

마지막 업데이트: 2026-04-17 (초기 상태)

## 검증기 현황

| 검증기 | 상태 | 비고 |
|--------|------|------|
| 01-build | ✅ 설정됨 | `./gradlew build -x test` |
| 02-test | ✅ 설정됨 | `./gradlew test` (Docker 필요) |
| 03-lint | ⚠️ 임시 | `./gradlew compileTestKotlin` — detekt/ktlint 미설정 |
| 04-security | ✅ 설정됨 | gitleaks 또는 패턴 매칭 |
| 05-docs | ✅ 설정됨 | CLAUDE.md 크기, exec-plan 정리 |

## 도메인별 테스트 커버리지

| 도메인 | 단위 테스트 | 통합 테스트 | 비고 |
|--------|------------|------------|------|
| booking | ✅ | ✅ | |
| occurrence | ✅ | ✅ | |
| organization | 부분 | 부분 | |
| instructor | 부분 | 부분 | |
| tour | 부분 | 부분 | |
| announcement | 부분 | 부분 | CommunicationReportingIntegrationTest 실패 중 (TD-002) |

## 미비 항목

- AuditEventTest 미존재 (TD-001)
- detekt/ktlint 미설정 (TD 추가 권장)
