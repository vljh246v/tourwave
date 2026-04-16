# Golden Principles — 에이전트가 지켜야 할 누적 규칙

`agent-failures.md`에서 반복된 실패가 승격된 원칙 목록입니다.

## 승격 기준

- `agent-failures.md`에서 같은 패턴 2회 이상 → 이 문서에 원칙 추가
- 같은 패턴 3회 이상 → 린트/테스트/검증기로 자동 강제 필수

---

## 원칙 목록

### GP-001: 워크트리 없이 소스 코드를 수정하지 않는다

- **왜**: 워크트리 밖 수정은 develop을 직접 오염시킴
- **대신**: `./scripts/task-start.sh <task-id>` 실행 후 워크트리에서 작업
- **강제**: `.claude/hooks/pre-tool-use.sh` — src/ 수정 시 워크트리 여부 확인

### GP-002: domain 레이어에 Spring/JPA/Web import를 포함하지 않는다

- **왜**: 헥사고날 아키텍처 의존성 방향 위반 — domain은 순수 Kotlin이어야 함
- **대신**: Spring 어노테이션이 필요하면 adapter 또는 application 레이어에서 처리
- **강제**: (아직 문서만 — ArchUnit 테스트로 격상 예정)

### GP-003: application 레이어가 adapter.out 구체 클래스를 직접 import하지 않는다

- **왜**: 헥사고날 아키텍처의 Port 추상화 목적이 무너짐
- **대신**: Port 인터페이스에만 의존. 구체 구현은 bootstrap에서 주입
- **강제**: (아직 문서만 — ArchUnit 테스트로 격상 예정)

### GP-004: 상태 변경 엔드포인트에 Idempotency-Key 처리를 빠뜨리지 않는다

- **왜**: `docs/domain-rules.md` 핵심 규칙 — 중복 요청 처리 누락 시 데이터 정합성 오류
- **대신**: 예약 create/approve/reject/cancel, offer accept/decline, 문의 create/message/close 모두 필수
- **강제**: `02-test.sh` — 통합 테스트에서 Idempotency 시나리오 검증

### GP-005: .env / 시크릿 파일을 git에 포함하지 않는다

- **왜**: git history에 한 번이라도 들어가면 완전 제거가 어려움
- **대신**: `.gitignore`에 등록 + 환경 변수 사용
- **강제**: `04-security.sh` — `git ls-files`로 커밋된 .env 감지

### GP-006: 새 Flyway 마이그레이션 파일은 반드시 사람 승인을 받는다

- **왜**: `escalation-policy.md#1` — 마이그레이션은 되돌리기 어려움
- **대신**: exec-plan에 Impact/Rollback/Verification 작성 후 승인
- **강제**: `escalation-policy.md` 에스컬레이션 절차

> 새 원칙 추가 시 `GP-{N+1}` 번호를 부여하고, 반드시 **왜/대신/강제** 세 항목을 채우세요.
