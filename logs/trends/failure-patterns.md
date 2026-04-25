# failure-patterns.md — 실패 패턴 분석

GC 에이전트가 `logs/validators/history.jsonl`을 분석하여 주기적으로 업데이트합니다.
**새 작업 시작 전에 이 파일을 읽어 과거 실패를 미리 파악하세요.**

마지막 분석: (초기 상태)
분석 기간: 최근 30일
총 태스크: 0 / 성공: 0 / 실패: 0

---

## 자주 실패하는 검증기

(데이터가 쌓이면 GC 에이전트가 아래 테이블을 업데이트합니다)

| 검증기 | 실패율 | 주요 원인 | 예방 방법 |
|--------|--------|-----------|-----------|
| - | - | - | - |

---

## 반복 오류 패턴

(GC 에이전트가 분석하여 추가합니다)

---

## Tourwave 주의사항 (초기 시드)

- **Testcontainers 필요**: `02-test.sh`는 Docker가 실행 중이어야 통과함
- **Flyway 마이그레이션**: 새 마이그레이션 파일은 `escalation-policy.md#1` 에스컬레이션 필요
- **헥사고날 경계**: `domain` 레이어에 Spring import 추가 시 아키텍처 위반

## 최근 관찰 패턴 (수동 추가)

- **2026-04-26 / 워크트리 미커밋 잔존 (T-001)** — 서브에이전트가 구현 후 git commit 누락 → task-finish.sh 빈 merge. 검증기는 작업트리 파일을 직접 읽어 PASS하므로 검출 못 함. `agent-failures.md#F-2026-04-26-1` 참조. 신규 작업 시작 전 워크트리 `git status` 청결 확인 권장.
- **2026-04-26 / 워크트리 외부 수정 누출 (T-002)** — F-2026-04-26-1과 같은 날 2회째. orchestrator/서브에이전트가 docs/audit/BE-auth.md를 메인 repo 절대경로로 수정. 워크트리 commit 메시지엔 포함 주장이나 실제 커밋 stat 미포함. `agent-failures.md#F-2026-04-26-2` 참조. **golden-principles GP-007 등재**. 3회 반복 시 pre-tool-use hook 구조적 차단 필요.
