# CLAUDE.md — tourwave

에이전트용 지도. 100줄 이하 유지. 세부 내용은 링크된 문서 참고.

## 프로젝트

투어/액티비티 운영사를 위한 예약 플랫폼 백엔드. Spring Boot 3.3.1 / Kotlin 1.9.24 / JDK 17, MySQL + Flyway, Spring Security (JWT), Micrometer + Prometheus.

## 작업 진입점

| 작업 유형 | 먼저 읽을 문서 | 시작 명령 |
|---|---|---|
| 새 기능 | `ARCHITECTURE.md` → `docs/golden-principles.md` | `/harness-task <id> <설명>` |
| 버그 수정 | `logs/trends/failure-patterns.md` → `docs/agent-failures.md` | `/harness-task <id> <설명>` |
| 리팩터링 | `ARCHITECTURE.md` → `docs/design-docs/index.md` | `/harness-task <id> <설명>` |
| 고위험 변경 | `docs/escalation-policy.md` → 사람 승인 필수 | - |
| 하네스 개선 | `docs/design-docs/core-beliefs.md` → `docs/golden-principles.md` | - |

## 워크플로우

```
/harness-task <task-id> <설명>
  → task-start.sh → (exec-plan 자동 생성) → 구현 → verify-task.sh → task-finish.sh
```

- 시작: `./scripts/task-start.sh <task-id>` — 워크트리 생성
- 중간 검증: `./scripts/verify-task.sh <task-id>` — 병합 안 함
- 완료: `./scripts/task-finish.sh <task-id>` — push + PR 안내

## 빌드 & 테스트

설정: `harness.config.sh` — BUILD/TEST/LINT 명령어 정의.

```bash
./gradlew test                          # 전체 테스트 (Docker 필요)
./gradlew test --tests "*.<Class>"       # 단일 테스트
```

상세 테스트 명령과 드리프트 가드: `docs/testing.md §6`.

## 핵심 문서

**스펙 우선순위**: `domain-rules.md` > `architecture.md` > `policies.md` > `schema.md` > `openapi.yaml` (충돌 시 domain-rules 우선)

### 프로젝트 스펙

| 문서 | 역할 |
|---|---|
| `ARCHITECTURE.md` | 레이어 구조, 패키지, 의존성 규칙 (하네스 린트 참조) |
| `docs/domain-rules.md` | 상태 머신, 불변식, 아이덤포턴시, 감사 이벤트, 환불 |
| `docs/architecture.md` | 헥사고날 가드레일, 트랜잭션 규칙, PR 체크리스트, 런타임 부트스트랩 |
| `docs/policies.md` | 인증/역할 매트릭스, 에러코드, 운영 정책 |
| `docs/schema.md` | MySQL 스키마 규약 |
| `docs/openapi.yaml` | HTTP 계약 SSOT |
| `docs/testing.md` | 테스트 규율, 시나리오 매트릭스, 커버리지 gap |
| `docs/operations.md` | 런타임 토폴로지, 지표, 출시 체크리스트 |

### 하네스

| 문서 | 역할 |
|---|---|
| `docs/golden-principles.md` | 실패에서 승격된 규칙 (GP-001~) |
| `docs/escalation-policy.md` | 사람 승인 필수 변경 |
| `docs/agent-failures.md` | 에이전트 실패 로그 |
| `docs/design-docs/core-beliefs.md` | 팀 원칙 |
| `docs/design-docs/index.md` | ADR 목록 |
| `docs/QUALITY_SCORE.md` | 도메인별 품질 현황 |
| `docs/exec-plans/active/` | 진행 중 exec-plan |

## 응답 스타일

군더더기 제거. 단편 문장 OK. 결과 먼저, 설명은 비자명한 경우만.
금지: "I will now", "Let me", "Great", 사과 표현.
진행 상황: 1줄 최대. 오류: 원인 + 수정만.

## 금지사항

→ `docs/golden-principles.md`

## 로그

- 세션: `logs/sessions/<task>/session.jsonl`
- 실패 패턴: `logs/trends/failure-patterns.md`
- 검증기 이력: `logs/validators/history.jsonl`
