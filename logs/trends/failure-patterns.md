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
- **2026-04-26 / 워크트리 미커밋 잔존 재발 (T-904)** — F-2026-04-26-1과 동일 패턴 같은 날 3회째 도달. tdd-backend가 23개 파일(구현 12 + 테스트 11) 변경 후 commit 누락, task-finish.sh 1차 빈 merge. 사람이 수동 commit 후 2차 시도로 정상 완료. `agent-failures.md#F-2026-04-26-3` 참조. **3회 도달 → 구조적 차단 의무화 트리거**. 즉시 조치: (1) task-finish.sh 시작부 워크트리 git status 가드, (2) tdd-backend.md "stage별 commit" 명시, (3) pre-tool-use hook 도입 (T-907 신규 카드 후보).
- **2026-04-27 / 작업 완료 후 문서·정합성 누락 (T-006)** — tdd-backend가 카드 WRITE 경로(`adapter/in/web/user/MeControllerIntegrationTest.kt`)대로 파일 생성 후, 실제 MeController는 `adapter/in/web/auth/` 패키지임을 알아채고 `auth/`에도 동일 7케이스 생성. user/ 잔재 미정리, verify-task.sh는 둘 다 PASS이라 검출 못함. 동시에 `docs/exec-plans/completed/T-006.md`는 빈 템플릿("[작업 목표를 작성하세요]")인 채로 archived. **세 결함 표면화**: (a) Phase 7.5는 SSOT(openapi/schema/policies/domain-rules)만 sync, audit/gap-matrix/exec-plan 본문은 미커버; (b) exec-plan active→completed 이동 시 본문 작성 단계 부재; (c) 카드 WRITE 경로의 src 대응 검증 부재. 즉시 조치: 사후 정리 commit `3c4a38b`로 user/ 중복 삭제. **T-907에 Phase 7.6/0 가드 추가 + Phase 8.5 cleanup 단계 신설 요구사항 등재**.
